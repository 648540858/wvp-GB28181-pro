package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 用单个服务节点Hash持久化下载任务，在同一事务中保留设备名额与任务版本。 */
@Component
public class SegmentedDownloadStore {
    private static final int MAX_RETRIES = 6;
    private final StringRedisTemplate redis;
    private final String serverId;
    private final String key;

    /** 固定服务归属，防止调用方指定其他节点的任务Hash或写入任意键。 */
    public SegmentedDownloadStore(StringRedisTemplate redis, UserSetting setting) {
        if (redis == null || setting == null || setting.getServerId() == null || setting.getServerId().isBlank()) {
            throw new IllegalArgumentException("下载任务存储缺少服务节点配置");
        }
        this.redis = redis;
        this.serverId = setting.getServerId();
        this.key = "WVP_SEGMENTED_DOWNLOAD_" + serverId;
    }

    /** 原子复用同区间任务或认领设备容量，任何存储故障都不能被解释为设备空闲。 */
    public SegmentedDownloadTask reserve(SegmentedDownloadTask task) {
        validate(task);
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            SegmentedDownloadTask result = redis.execute(new SessionCallback<>() {
                /** WATCH覆盖整个任务Hash，另一流认领同一设备也会使本次提交失败。 */
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> SegmentedDownloadTask execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.watch(key);
                    boolean transactionOpen = false;
                    try {
                        SegmentedDownloadTask current = decode(ops.opsForHash().get(key, task.getStream()));
                        if (current != null && (current.isBusy() || current.getState() == SegmentedDownloadTask.State.COMPLETED)) {
                            ops.unwatch();
                            return current;
                        }
                        for (Object value : ops.opsForHash().values(key)) {
                            SegmentedDownloadTask other = decode(value);
                            if (!Objects.equals(task.getStream(), other.getStream())
                                    && Objects.equals(task.getDeviceId(), other.getDeviceId()) && other.occupiesDevice()) {
                                throw new IllegalStateException("设备已有未结束或未确认释放的下载任务");
                            }
                        }
                        SegmentedDownloadTask candidate = snapshot(task);
                        candidate.setServerId(serverId);
                        candidate.setVersion(1);
                        long now = System.currentTimeMillis();
                        candidate.setCreatedAt(candidate.getCreatedAt() > 0 ? candidate.getCreatedAt() : now);
                        candidate.setUpdatedAt(now);
                        String encoded = JSON.toJSONString(candidate);
                        ops.multi();
                        transactionOpen = true;
                        ops.opsForHash().put(key, candidate.getStream(), encoded);
                        List<Object> committed = ops.exec();
                        transactionOpen = false;
                        return wasCommitted(committed) ? decode(encoded) : null;
                    } catch (RuntimeException error) {
                        abort(ops, transactionOpen, error);
                        throw error;
                    }
                }
            });
            if (result != null) return result;
        }
        throw new IllegalStateException("下载任务认领发生持续并发冲突");
    }

    /** 仅更新相同任务身份和预期版本，成功提交后才推进调用方快照版本。 */
    public boolean save(SegmentedDownloadTask task, long expectedVersion) {
        validate(task);
        if (expectedVersion < 1 || expectedVersion == Long.MAX_VALUE) return false;
        long updatedAt = System.currentTimeMillis();
        SegmentedDownloadTask candidate = snapshot(task);
        candidate.setServerId(serverId);
        candidate.setVersion(expectedVersion + 1);
        candidate.setUpdatedAt(updatedAt);
        String encoded = JSON.toJSONString(candidate);
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            Boolean result = redis.execute(new SessionCallback<>() {
                /** 比较身份与版本和写回处于同一连接，事务冲突不能覆盖另一工作者的新状态。 */
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Boolean execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.watch(key);
                    boolean transactionOpen = false;
                    try {
                        SegmentedDownloadTask current = decode(ops.opsForHash().get(key, task.getStream()));
                        if (!isCurrent(current, task, expectedVersion)) {
                            ops.unwatch();
                            return false;
                        }
                        ops.multi();
                        transactionOpen = true;
                        ops.opsForHash().put(key, task.getStream(), encoded);
                        List<Object> committed = ops.exec();
                        transactionOpen = false;
                        return wasCommitted(committed) ? true : null;
                    } catch (RuntimeException error) {
                        abort(ops, transactionOpen, error);
                        throw error;
                    }
                }
            });
            if (result != null) {
                if (result) {
                    task.setVersion(expectedVersion + 1);
                    task.setUpdatedAt(updatedAt);
                    task.setServerId(serverId);
                }
                return result;
            }
        }
        return false;
    }

    /** 返回独立反序列化快照，查询失败或损坏数据不能退化为空任务。 */
    public SegmentedDownloadTask get(String stream) {
        if (stream == null || stream.isBlank()) throw new IllegalArgumentException("缺少下载任务流标识");
        return decode(redis.opsForHash().get(key, stream));
    }

    /** 只遍历固定服务Hash，不扫描全局键空间，不修改任务保留期限。 */
    public List<SegmentedDownloadTask> list() {
        List<SegmentedDownloadTask> tasks = new ArrayList<>();
        for (Object value : redis.opsForHash().values(key)) tasks.add(decode(value));
        return tasks;
    }

    /** 只删除仍属于该身份和版本的安全终态，过期工作者不能抹掉替代任务或未释放媒体。 */
    public boolean deleteIfCurrent(SegmentedDownloadTask task) {
        validate(task);
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            Boolean result = redis.execute(new SessionCallback<>() {
                /** 终态及设备安全状态也在事务内复核，不能依赖清理扫描时的旧快照。 */
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Boolean execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.watch(key);
                    boolean transactionOpen = false;
                    try {
                        SegmentedDownloadTask current = decode(ops.opsForHash().get(key, task.getStream()));
                        if (!isCurrent(current, task, task.getVersion()) || !current.isTerminal() || !current.isSafeToStart()) {
                            ops.unwatch();
                            return false;
                        }
                        ops.multi();
                        transactionOpen = true;
                        ops.opsForHash().delete(key, task.getStream());
                        List<Object> committed = ops.exec();
                        transactionOpen = false;
                        return wasCommitted(committed) ? true : null;
                    } catch (RuntimeException error) {
                        abort(ops, transactionOpen, error);
                        throw error;
                    }
                }
            });
            if (result != null) return result;
        }
        return false;
    }

    /** Spring/Lettuce的WATCH冲突可能返回空列表，必须收到本事务唯一命令的结果才算提交。 */
    private boolean wasCommitted(List<Object> results) {
        if (results == null || results.isEmpty()) {
            return false;
        }
        if (results.size() != 1 || results.getFirst() == null || results.getFirst() instanceof Throwable) {
            throw new IllegalStateException("下载任务事务结果不可确认");
        }
        return true;
    }

    /** 限定节点与必需身份，序列化之前拒绝无法安全认领或恢复的任务。 */
    private void validate(SegmentedDownloadTask task) {
        if (task == null || task.getId() == null || task.getId().isBlank()
                || task.getStream() == null || task.getStream().isBlank()
                || task.getDeviceId() == null || task.getDeviceId().isBlank() || task.getState() == null
                || task.getServerId() != null && !serverId.equals(task.getServerId())) {
            throw new IllegalArgumentException("下载任务缺少身份或不属于当前服务节点");
        }
    }

    /** 校验存储快照，损坏JSON或错误节点归属必须阻止继续建流。 */
    private SegmentedDownloadTask decode(Object raw) {
        if (raw == null) return null;
        try {
            if (!(raw instanceof String)) throw new IllegalArgumentException("非字符串任务数据");
            SegmentedDownloadTask task = JSON.parseObject((String) raw, SegmentedDownloadTask.class);
            validate(task);
            if (!serverId.equals(task.getServerId()) || task.getVersion() < 1) {
                throw new IllegalArgumentException("任务节点或版本无效");
            }
            return task;
        } catch (RuntimeException error) {
            throw new IllegalStateException("下载任务存储内容不可用", error);
        }
    }

    /** 用实际存储协议制作深快照，调用方的后续修改不会改变已认领任务。 */
    private SegmentedDownloadTask snapshot(SegmentedDownloadTask task) {
        return JSON.parseObject(JSON.toJSONString(task), SegmentedDownloadTask.class);
    }

    /** 身份与版本必须同时一致，安全状态或任务阶段相同不足以证明写入归属。 */
    private boolean isCurrent(SegmentedDownloadTask current, SegmentedDownloadTask task, long version) {
        return current != null && Objects.equals(current.getId(), task.getId())
                && Objects.equals(current.getStream(), task.getStream())
                && Objects.equals(current.getDeviceId(), task.getDeviceId()) && current.getVersion() == version;
    }

    /** 失败时清除连接的WATCH或MULTI状态，原始存储故障始终向调用方传播。 */
    private void abort(RedisOperations<String, String> ops, boolean transactionOpen, RuntimeException original) {
        try {
            if (transactionOpen) ops.discard(); else ops.unwatch();
        } catch (RuntimeException cleanup) {
            original.addSuppressed(cleanup);
        }
    }
}
