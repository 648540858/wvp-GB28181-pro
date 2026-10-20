package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 视频流session管理器，管理视频预览、预览回放的通信句柄
 *
 * 说明：
 * - CALL_ID Hash 保存全部 Dialog 事务，是停止/清理的权威数据源；
 * - STREAM Hash 只保存每个 app+stream 的最新事务，作为快速查询指针；
 * - STREAM 被新事务覆盖时，旧事务仍然保留在 CALL_ID 中，停止时按通道/类型扫描即可找到。
 */
@Slf4j
@Component
public class SipInviteSessionManager {

    private static final int MAX_RETRY = 3;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String streamKey() {
        return VideoManagerConstants.SIP_INVITE_SESSION_STREAM + userSetting.getServerId();
    }

    private String callIdKey() {
        return VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId();
    }

    /**
     * 添加/更新一个点播/回放的事务信息。
     *
     * 两个 Hash 在同一个 MULTI/EXEC 中写入，避免只写了一半：
     * - STREAM: app+stream -> 事务
     * - CALL_ID: callId -> 事务
     */
    public void put(SsrcTransaction ssrcTransaction) {
        if (ssrcTransaction == null) {
            return;
        }
        if (ssrcTransaction.getCallId() == null) {
            log.warn("[SIP事务] callId 为空，忽略写入: app={}, stream={}",
                    ssrcTransaction.getApp(), ssrcTransaction.getStream());
            return;
        }
        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Void execute(RedisOperations<K, V> operations) {
                RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                ops.multi();
                if (ssrcTransaction.getApp() != null && ssrcTransaction.getStream() != null) {
                    ops.opsForHash().put(streamKey(),
                            ssrcTransaction.getApp() + ssrcTransaction.getStream(), ssrcTransaction);
                }
                ops.opsForHash().put(callIdKey(), ssrcTransaction.getCallId(), ssrcTransaction);
                ops.exec();
                return null;
            }
        });
    }

    public SsrcTransaction getSsrcTransactionByStream(String app, String stream) {
        if (app == null || stream == null) {
            return null;
        }
        return (SsrcTransaction) redisTemplate.opsForHash().get(streamKey(), app + stream);
    }

    /**
     * 按 Call-ID 查询事务。
     *
     * 历史版本的 PLAYBACK/TALK 可能把错误的 Call-ID 存成了 Hash field，
     * 这里在直接查询失败时用 SipTransactionInfo.callId 兜底扫描，避免旧数据永远查不到。
     */
    public SsrcTransaction getSsrcTransactionByCallId(String callId) {
        if (callId == null) {
            return null;
        }
        SsrcTransaction direct = (SsrcTransaction) redisTemplate.opsForHash().get(callIdKey(), callId);
        if (direct != null) {
            return direct;
        }
        for (SsrcTransaction transaction : getAll()) {
            if (transaction != null && transaction.getSipTransactionInfo() != null
                    && Objects.equals(callId, transaction.getSipTransactionInfo().getCallId())) {
                return transaction;
            }
            if (transaction != null && Objects.equals(callId, transaction.getCallId())) {
                return transaction;
            }
        }
        for (SsrcTransaction transaction : getAllStreamIndex()) {
            if (transaction != null && Objects.equals(callId, transaction.getCallId())) {
                return transaction;
            }
            if (transaction != null && transaction.getSipTransactionInfo() != null
                    && Objects.equals(callId, transaction.getSipTransactionInfo().getCallId())) {
                return transaction;
            }
        }
        return null;
    }

    public List<SsrcTransaction> getSsrcTransactionByDeviceId(String deviceId) {
        if (deviceId == null) {
            return new ArrayList<>();
        }
        Map<String, SsrcTransaction> result = new LinkedHashMap<>();
        addAll(result, getAll());
        addAll(result, getAllStreamIndex());
        List<SsrcTransaction> transactions = new ArrayList<>();
        for (SsrcTransaction transaction : result.values()) {
            if (transaction != null && Objects.equals(deviceId, transaction.getDeviceId())) {
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    /**
     * 按 app + stream 查询全部事务（可能包含设备侧和平台侧）。
     */
    public List<SsrcTransaction> getSsrcTransactionsByStream(String app, String stream) {
        List<SsrcTransaction> result = new ArrayList<>();
        for (SsrcTransaction transaction : getAll()) {
            if (transaction != null
                    && Objects.equals(app, transaction.getApp())
                    && Objects.equals(stream, transaction.getStream())) {
                result.add(transaction);
            }
        }
        return result;
    }

    /**
     * 按设备 + 通道 + 类型查询全部设备侧事务。
     * type 为 null 时不按类型过滤。
     */
    public List<SsrcTransaction> getSsrcTransactionsByDeviceChannel(String deviceId, Integer channelId,
                                                                    InviteSessionType type) {
        List<SsrcTransaction> result = new ArrayList<>();
        for (SsrcTransaction transaction : getAll()) {
            if (transaction == null || transaction.getPlatformId() != null) {
                continue;
            }
            if (!Objects.equals(deviceId, transaction.getDeviceId())) {
                continue;
            }
            if (!Objects.equals(channelId, transaction.getChannelId())) {
                continue;
            }
            if (type != null && !Objects.equals(type, transaction.getType())) {
                continue;
            }
            result.add(transaction);
        }
        return result;
    }

    /**
     * 按平台 + 通道 + 类型查询全部平台侧事务。
     * type 为 null 时不按类型过滤。
     */
    public List<SsrcTransaction> getSsrcTransactionsByPlatformChannel(String platformId, Integer channelId,
                                                                      InviteSessionType type) {
        List<SsrcTransaction> result = new ArrayList<>();
        for (SsrcTransaction transaction : getAll()) {
            if (transaction == null || transaction.getPlatformId() == null) {
                continue;
            }
            if (!Objects.equals(platformId, transaction.getPlatformId())) {
                continue;
            }
            if (!Objects.equals(channelId, transaction.getChannelId())) {
                continue;
            }
            if (type != null && !Objects.equals(type, transaction.getType())) {
                continue;
            }
            result.add(transaction);
        }
        return result;
    }

    /**
     * 按流媒体节点查询全部事务。
     */
    public List<SsrcTransaction> getSsrcTransactionsByMediaServer(String mediaServerId) {
        List<SsrcTransaction> result = new ArrayList<>();
        for (SsrcTransaction transaction : getAll()) {
            if (transaction != null && Objects.equals(mediaServerId, transaction.getMediaServerId())) {
                result.add(transaction);
            }
        }
        return result;
    }

    /**
     * STREAM Hash 中的全部最新事务，用于和 CALL_ID 做并集，兼容半写数据。
     */
    public List<SsrcTransaction> getAllStreamIndex() {
        List<Object> values = redisTemplate.opsForHash().values(streamKey());
        List<SsrcTransaction> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (Object value : values) {
            if (value instanceof SsrcTransaction) {
                result.add((SsrcTransaction) value);
            }
        }
        return result;
    }

    /**
     * 按 callId 删除事务。删除 STREAM 索引前先确认它仍然指向当前事务，
     * 避免清理旧 Call-ID 时误删新会话的 stream 索引。
     *
     * 使用 WATCH/MULTI 保证“检查 + 删除”原子化。
     */
    public boolean removeByCallId(String callId) {
        if (callId == null) {
            return false;
        }
        SsrcTransaction transaction = getSsrcTransactionByCallId(callId);
        if (transaction == null) {
            return false;
        }
        String storedCallId = transaction.getCallId() != null ? transaction.getCallId() : callId;
        String field = transaction.getApp() + transaction.getStream();
        boolean hasStream = transaction.getApp() != null && transaction.getStream() != null;

        for (int i = 0; i < MAX_RETRY; i++) {
            Boolean result = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Boolean execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                    ops.watch(streamKey());
                    SsrcTransaction indexed = (SsrcTransaction) ops.opsForHash().get(streamKey(), field);
                    ops.multi();
                    if (hasStream && isStreamIndexOwnedBy(indexed, storedCallId)) {
                        ops.opsForHash().delete(streamKey(), field);
                    }
                    ops.opsForHash().delete(callIdKey(), storedCallId);
                    return ops.exec() != null;
                }
            });
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }
        log.warn("[SIP事务] 删除 callId={} 重试 {} 次后仍未成功", callId, MAX_RETRY);
        return false;
    }

    /**
     * 仅当 STREAM 索引仍指向指定 Call-ID 时删除。
     */
    public boolean removeByStreamIfCallId(String app, String stream, String callId) {
        if (app == null || stream == null || callId == null) {
            return false;
        }
        String field = app + stream;
        for (int i = 0; i < MAX_RETRY; i++) {
            Boolean result = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Boolean execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                    ops.watch(streamKey());
                    SsrcTransaction indexed = (SsrcTransaction) ops.opsForHash().get(streamKey(), field);
                    if (!isStreamIndexOwnedBy(indexed, callId)) {
                        ops.unwatch();
                        return false;
                    }
                    ops.multi();
                    ops.opsForHash().delete(streamKey(), field);
                    if (indexed.getCallId() != null) {
                        ops.opsForHash().delete(callIdKey(), indexed.getCallId());
                    }
                    return ops.exec() != null;
                }
            });
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 仅当 STREAM 索引仍指向指定 SSRC 时删除。
     * 用于错误/超时回调，避免旧 INVITE 的迟到回调误删新会话。
     */
    public boolean removeByStreamIfSsrc(String app, String stream, String ssrc) {
        if (app == null || stream == null) {
            return false;
        }
        String field = app + stream;
        for (int i = 0; i < MAX_RETRY; i++) {
            Boolean result = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Boolean execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                    ops.watch(streamKey());
                    SsrcTransaction indexed = (SsrcTransaction) ops.opsForHash().get(streamKey(), field);
                    if (indexed == null || !Objects.equals(ssrc, indexed.getSsrc())) {
                        ops.unwatch();
                        return false;
                    }
                    ops.multi();
                    ops.opsForHash().delete(streamKey(), field);
                    if (indexed.getCallId() != null) {
                        ops.opsForHash().delete(callIdKey(), indexed.getCallId());
                    }
                    return ops.exec() != null;
                }
            });
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @deprecated 使用 {@link #removeByCallId(String)} 或条件删除。
     * 保留该方法是为了兼容旧调用，内部按当前 stream 指向的事务删除。
     */
    @Deprecated
    public void removeByStream(String app, String stream) {
        SsrcTransaction ssrcTransaction = getSsrcTransactionByStream(app, stream);
        if (ssrcTransaction != null) {
            removeByCallId(ssrcTransaction.getCallId());
        }
    }

    /**
     * 判断 stream 索引当前是否仍属于指定的事务(Call-ID)
     */
    public static boolean isStreamIndexOwnedBy(SsrcTransaction indexed, String callId) {
        return indexed != null && callId != null && callId.equals(indexed.getCallId());
    }

    public List<SsrcTransaction> getAll() {
        List<Object> values = redisTemplate.opsForHash().values(callIdKey());
        List<SsrcTransaction> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (Object value : values) {
            if (value instanceof SsrcTransaction) {
                result.add((SsrcTransaction) value);
            }
        }
        return result;
    }

    private void addAll(Map<String, SsrcTransaction> result, List<SsrcTransaction> transactions) {
        if (transactions == null) {
            return;
        }
        for (SsrcTransaction transaction : transactions) {
            if (transaction == null) {
                continue;
            }
            String key = transaction.getCallId();
            if (key == null && transaction.getSipTransactionInfo() != null) {
                key = transaction.getSipTransactionInfo().getCallId();
            }
            if (key != null) {
                result.putIfAbsent(key, transaction);
            }
        }
    }
}
