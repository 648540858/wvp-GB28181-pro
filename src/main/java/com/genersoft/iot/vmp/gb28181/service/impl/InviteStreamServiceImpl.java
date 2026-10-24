package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class InviteStreamServiceImpl implements IInviteStreamService {

    /**
     * 点播等待回调。key = {@code type:channelId[:stream]}，value 同时保存回调列表和认领信息。
     *
     * <p>这里刻意把“等待回调”和“认领创建时间”放在同一个 Holder 里，而不是拆成两个 Map。
     * 因为两者本来就是同一条逻辑记录：只要回调还在，就说明这个点播请求还处于“在途”状态；
     * 一旦回调被移除，对应的时间记录也必须一起消失。拆成两个 Map 时，任何一处只清理了其中一个，
     * 就会留下“回调没了但时间还在”或“时间没了但回调还在”的脏数据，兜底逻辑也会因此失效。</p>
     */
    private final Map<String, InviteCallbackHolder> inviteCallbackMap = new ConcurrentHashMap<>();

    /**
     * 一次点播等待的上下文：认领时间、归属信息、所有等待中的回调。
     *
     * <p>归属信息（type/channelId/stream）用于兜底清理时回收残留的 Redis 点播信息和通道播放状态。</p>
     */
    private static final class InviteCallbackHolder {

        /** 认领时间（首次注册回调的时间），用于判断是否已经等待过久 */
        private final long createTime;
        /** 归属信息，仅用于兜底清理 */
        private final InviteSessionType type;
        private final Integer channelId;
        private final String stream;
        /** 等待本次点播结果的所有回调，并发请求会不断加入 */
        private final List<ErrorCallback<StreamInfo>> callbacks = new CopyOnWriteArrayList<>();

        private InviteCallbackHolder(InviteSessionType type, Integer channelId, String stream) {
            this.createTime = System.currentTimeMillis();
            this.type = type;
            this.channelId = channelId;
            this.stream = stream;
        }
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    /**
     * 流离开的处理
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema()) && MediaStreamUtil.isGB28181(event.getApp(), event.getStream())) {
            // 优先按点播/回放查找：保留中的下载会话与回放共用同一流名时不能被随机命中
            InviteInfo inviteInfo = getInviteInfoByStreamPreferPlay(event.getStream());
            if (inviteInfo != null && (inviteInfo.getType() == InviteSessionType.PLAY
                    || inviteInfo.getType() == InviteSessionType.PLAYBACK)) {
                if (isStaleStreamDeparture(inviteInfo, event.getCreateStamp())) {
                    log.info("[流离开] 流实例创建时间早于当前会话，判定为旧会话的迟到注销事件，跳过清理: stream={}, deviceId={}, channelId={}",
                            event.getStream(), inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                    return;
                }
                try {
                    if (inviteInfo.getStatus() != InviteSessionStatus.ok) {
                        removeInviteInfo(inviteInfo);
                    }
                    Device device = deviceMapper.getDeviceByDeviceId(inviteInfo.getDeviceId());
                    if (device != null) {
                        deviceChannelMapper.stopPlayById(inviteInfo.getChannelId());
                    }
                } catch (Exception e) {
                    log.error("[流离开] 清理Invite异常: deviceId={}, channelId={}, stream={}",
                            inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream(), e);
                }
            }
        }
    }

    @Override
    public void updateInviteInfo(InviteInfo inviteInfo) {
        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            updateInviteInfo(inviteInfo, Long.valueOf(userSetting.getPlayTimeout()) * 2);
        } else {
            updateInviteInfo(inviteInfo, null);
        }
    }

    @Override
    public void setInviteInfoCleanTime(InviteInfo inviteInfo, long keepTimeMs) {
        if (inviteInfo == null || inviteInfo.getType() == null || inviteInfo.getChannelId() == null
                || inviteInfo.getStream() == null || keepTimeMs <= 0) {
            log.warn("[设置Invite保留窗口]，参数不全： {}", JSON.toJSON(inviteInfo));
            return;
        }
        InviteInfo inviteInfoInRedis = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInRedis == null) {
            log.warn("[设置Invite保留窗口]，未从缓存中读取到Invite信息： deviceId: {}, channel: {}, stream: {}",
                    inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
            return;
        }
        long cleanTime = System.currentTimeMillis() + keepTimeMs;
        inviteInfoInRedis.setCleanTime(cleanTime);
        if (inviteInfoInRedis.getCreateTime() == null) {
            inviteInfoInRedis.setCreateTime(System.currentTimeMillis());
        }
        String key = VideoManagerConstants.INVITE_PREFIX;
        String objectKey = inviteInfoInRedis.getType() +
                ":" + inviteInfoInRedis.getChannelId() +
                ":" + inviteInfoInRedis.getStream();
        redisTemplate.opsForHash().put(key, objectKey, inviteInfoInRedis);
    }

    @Override
    public void updateInviteInfo(InviteInfo inviteInfo, Long time) {
        if (inviteInfo == null || (inviteInfo.getDeviceId() == null || inviteInfo.getChannelId() == null)) {
            log.warn("[更新Invite信息]，参数不全： {}", JSON.toJSON(inviteInfo));
            return;
        }
        InviteInfo inviteInfoForUpdate;

        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            if (inviteInfo.getDeviceId() == null || inviteInfo.getChannelId() == null
                    || inviteInfo.getType() == null || inviteInfo.getStream() == null
            ) {
                return;
            }
            inviteInfoForUpdate = inviteInfo;
        } else {
            InviteInfo inviteInfoInRedis = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
            if (inviteInfoInRedis == null) {
                log.warn("[更新Invite信息]，未从缓存中读取到Invite信息： deviceId: {}, channel: {}, stream: {}",
                        inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
                return;
            }
            if (inviteInfo.getStreamInfo() != null) {
                inviteInfoInRedis.setStreamInfo(inviteInfo.getStreamInfo());
            }
            if (inviteInfo.getSsrcInfo() != null) {
                inviteInfoInRedis.setSsrcInfo(inviteInfo.getSsrcInfo());
            }
            if (inviteInfo.getStreamMode() != null) {
                inviteInfoInRedis.setStreamMode(inviteInfo.getStreamMode());
            }
            if (inviteInfo.getReceiveIp() != null) {
                inviteInfoInRedis.setReceiveIp(inviteInfo.getReceiveIp());
            }
            if (inviteInfo.getReceivePort() != null) {
                inviteInfoInRedis.setReceivePort(inviteInfo.getReceivePort());
            }
            if (inviteInfo.getStatus() != null) {
                inviteInfoInRedis.setStatus(inviteInfo.getStatus());
            }

            inviteInfoForUpdate = inviteInfoInRedis;

        }
        if (inviteInfoForUpdate.getCreateTime() == null) {
            inviteInfoForUpdate.setCreateTime(System.currentTimeMillis());
        }
        String key = VideoManagerConstants.INVITE_PREFIX;
        String objectKey = inviteInfoForUpdate.getType() +
                ":" + inviteInfoForUpdate.getChannelId() +
                ":" + inviteInfoForUpdate.getStream();
        if (time != null && time > 0) {
            inviteInfoForUpdate.setExpirationTime(time);
        }
        redisTemplate.opsForHash().put(key, objectKey, inviteInfoForUpdate);
    }

    @Override
    public InviteInfo updateInviteInfoForStream(InviteInfo inviteInfo, String stream) {

        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInDb == null) {
            return null;
        }
        removeInviteInfo(inviteInfoInDb);
        String key = VideoManagerConstants.INVITE_PREFIX;
        String objectKey = inviteInfo.getType() +
                ":" + inviteInfo.getChannelId() +
                ":" + stream;
        inviteInfoInDb.setStream(stream);
        if (inviteInfoInDb.getSsrcInfo() != null) {
            inviteInfoInDb.getSsrcInfo().setStream(stream);
        }
        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            inviteInfoInDb.setExpirationTime((long) (userSetting.getPlayTimeout() * 2));
        }
        if (inviteInfoInDb.getCreateTime() == null) {
            inviteInfoInDb.setCreateTime(System.currentTimeMillis());
        }
        redisTemplate.opsForHash().put(key, objectKey, inviteInfoInDb);
        return inviteInfoInDb;
    }

    @Override
    public InviteInfo getInviteInfo(InviteSessionType type, Integer channelId, String stream) {
        String key = VideoManagerConstants.INVITE_PREFIX;
        String keyPattern = (type != null ? type : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*");
        ScanOptions options = ScanOptions.scanOptions().match(keyPattern).count(20).build();
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(key, options)) {
            if (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                cursor.close();
                if (entry.getValue() instanceof InviteInfo) {
                    return (InviteInfo) entry.getValue();
                } else {
                    log.warn("[Redis-InviteInfo] 发现脏数据并清理: key={}, value={}", entry.getKey(), entry.getValue());
                    redisTemplate.opsForHash().delete(key, entry.getKey());
                }
            }
        } catch (Exception e) {
            log.error("[Redis-InviteInfo] 查询异常: ", e);
        }
        return null;
    }

    @Override
    public List<InviteInfo> getAllInviteInfo() {
        List<InviteInfo> result = new ArrayList<>();
        String key = VideoManagerConstants.INVITE_PREFIX;
        List<Object> values = redisTemplate.opsForHash().values(key);
        if(values.isEmpty()) {
            return result;
        }
        for (Object value : values) {
            result.add((InviteInfo)value);
        }
        return result;
    }

    @Override
    public InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type, Integer channelId) {
        return getInviteInfo(type, channelId, null);
    }

    @Override
    public InviteInfo getInviteInfoByStream(InviteSessionType type, String stream) {
        return getInviteInfo(type, null, stream);
    }

    @Override
    public InviteInfo getInviteInfoByStreamPreferPlay(String stream) {
        InviteInfo inviteInfo = getInviteInfo(InviteSessionType.PLAY, null, stream);
        if (inviteInfo != null) {
            return inviteInfo;
        }
        inviteInfo = getInviteInfo(InviteSessionType.PLAYBACK, null, stream);
        if (inviteInfo != null) {
            return inviteInfo;
        }
        // 其他类型（含保留中的录像下载会话）维持原有的通配查询逻辑
        return getInviteInfoByStream(null, stream);
    }

    @Override
    public void removeInviteInfo(InviteSessionType type, Integer channelId, String stream) {
        // 注意：这里刻意不清理 inviteCallbackMap。
        // updateInviteInfoForSSRC() / updateInviteInfoForStream() 这类“更新”流程会先调用本方法，
        // 再把 InviteInfo 重新写回 Redis。如果在这里清理等待回调，单端口设备 200OK 中
        // SSRC 与请求不一致等场景会把仍在等待的请求丢掉，造成点播实际成功但 HTTP 请求超时。
        // 等待认领的释放统一由 call() 和兜底清理（cleanExpiredInviteCallbacks）负责。
        String key = VideoManagerConstants.INVITE_PREFIX;
        if (type == null && channelId == null && stream == null) {
            redisTemplate.delete(key);
            return;
        }
        String keyPattern = (type != null ? type : "*") + ":" + (channelId != null ? channelId : "*") + ":" + (stream != null ? stream : "*");
        ScanOptions options = ScanOptions.scanOptions().match(keyPattern).count(20).build();
        List<Object> objectKeys = new ArrayList<>();
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(key, options)) {
            while (cursor.hasNext()) {
                objectKeys.add(cursor.next().getKey());
            }
        } catch (Exception e) {
            log.error("[Redis-InviteInfo] 删除异常: ", e);
        }
        if (!objectKeys.isEmpty()) {
            redisTemplate.opsForHash().delete(key, objectKeys.toArray());
        }
    }

    @Override
    public void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, Integer channelId) {
        removeInviteInfo(inviteSessionType, channelId, null);
    }

    @Override
    public void removeInviteInfo(InviteInfo inviteInfo) {
        removeInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
    }

    @Override
    public void once(InviteSessionType type, Integer channelId, String stream, ErrorCallback<StreamInfo> callback) {
        registerCallback(type, channelId, stream, callback, null);
    }

    @Override
    public boolean onceAndFirst(InviteSessionType type, Integer channelId, String stream, ErrorCallback<StreamInfo> callback) {
        AtomicBoolean first = new AtomicBoolean(false);
        registerCallback(type, channelId, stream, callback, first);
        return first.get();
    }

    /**
     * 注册一个等待回调。
     *
     * <p>并发边界：这里使用 {@link ConcurrentHashMap#compute}，而不是
     * {@code computeIfAbsent} + 手动 {@code get(key).add(callback)}。
     * compute 对同一个 key 是原子的，只有两种结果：</p>
     * <ul>
     *     <li>key 不存在：创建 Holder（相当于“认领”本次点播），并把回调放进去；</li>
     *     <li>key 已存在：直接把回调追加到已有 Holder 上。</li>
     * </ul>
     * <p>如果拆成两步，可能出现：线程 A 通过 computeIfAbsent 拿到了回调列表，
     * 但在 add 之前，线程 B 已经通过 call()/兜底清理把整个 key 移除。
     * 此时 A 的回调会被加到一个已经不在 map 里的列表上，永远收不到通知，
     * 对应的 HTTP 请求只能等自己的 DeferredResult 超时。compute 不会出现这个窗口。</p>
     *
     * @param first 不为 null 时，用于回填“本次调用是否创建了新的认领（即是否是第一个请求）”
     */
    private void registerCallback(InviteSessionType type, Integer channelId, String stream,
                                  ErrorCallback<StreamInfo> callback, AtomicBoolean first) {
        String key = buildKey(type, channelId, stream);
        inviteCallbackMap.compute(key, (k, holder) -> {
            if (holder == null) {
                holder = new InviteCallbackHolder(type, channelId, stream);
                if (first != null) {
                    first.set(true);
                }
            }
            holder.callbacks.add(callback);
            return holder;
        });
    }

    private String buildKey(InviteSessionType type, Integer channelId, String stream) {
        String key = type + ":" + channelId;
        // 如果ssrc未null那么可以实现一个通道只能一次操作，ssrc不为null则可以支持一个通道多次invite
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }


    @Override
    public void clearInviteInfo(String deviceId) {
        List<InviteInfo> inviteInfoList = getAllInviteInfo();
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (inviteInfo.getDeviceId().equals(deviceId)) {
                removeInviteInfo(inviteInfo);
            }
        }
    }

    @Override
    public int getStreamInfoCount(String mediaServerId) {
        int count = 0;
        String key = VideoManagerConstants.INVITE_PREFIX;
        List<Object> values = redisTemplate.opsForHash().values(key);
        if (values.isEmpty()) {
            return count;
        }
        for (Object value : values) {
            InviteInfo inviteInfo = (InviteInfo)value;
            if (inviteInfo != null
                    && inviteInfo.getStreamInfo() != null
                    && inviteInfo.getStreamInfo().getMediaServer() != null
                    && inviteInfo.getStreamInfo().getMediaServer().getId().equals(mediaServerId)) {
                if (inviteInfo.getType().equals(InviteSessionType.DOWNLOAD) && inviteInfo.getStreamInfo().getProgress() == 1) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    @Override
    public void call(InviteSessionType type, Integer channelId, String stream, int code, String msg, StreamInfo data) {
        notifyAndRemove(buildKey(type, channelId, stream), code, msg, data);
    }

    /**
     * 移除指定点播请求的等待上下文，并通知所有等待者。
     *
     * <p>并发边界：call() 和兜底清理可能同时处理同一个 key。
     * 这里通过 {@code map.remove(key)} 的返回值来裁决“谁负责通知”：
     * 只有成功从 map 中移除 Holder 的一方才继续通知，另一方拿到的必然是 null，
     * 因此同一个 Holder 不会被通知两次（一次成功、一次超时）。</p>
     */
    private void notifyAndRemove(String key, int code, String msg, StreamInfo data) {
        InviteCallbackHolder holder = inviteCallbackMap.remove(key);
        if (holder == null) {
            return;
        }
        notifyCallbacks(key, holder, code, msg, data);
    }

    /**
     * 通知 Holder 中的所有等待者。
     *
     * <p>并发边界：等待回调来自业务方（Controller / RPC），执行过程中可能抛异常，
     * 也可能耗时较长。这里逐个回调 try/catch，保证：</p>
     * <ul>
     *     <li>一个回调异常不会影响其他等待同一结果的请求；</li>
     *     <li>异常不会向上传播到 SIP 事件线程或定时清理线程，导致后续清理逻辑被中断。</li>
     * </ul>
     */
    private void notifyCallbacks(String key, InviteCallbackHolder holder, int code, String msg, StreamInfo data) {
        if (holder.callbacks.isEmpty()) {
            return;
        }
        for (ErrorCallback<StreamInfo> callback : holder.callbacks) {
            if (callback == null) {
                continue;
            }
            try {
                callback.run(code, msg, data);
            } catch (Exception e) {
                log.error("[点播回调] 执行异常，已跳过该回调: key={}, code={}", key, code, e);
            }
        }
    }

    /**
     * 兜底清理长时间未结束的点播等待回调，防止"已有请求在途"永久残留：
     * 正常流程下点播结束(成功/失败/超时)都会调用call()释放回调，若某条释放路径断裂
     * (例如收流超时看门狗因为任务key复用没能注册上)，就没有任何线程调用call()，
     * 点播认领会永久残留，该通道之后的点播一直返回"已有请求在途"。
     * 这里在超过 playTimeout*3 后释放认领并通知等待者，保证后续点播能够重新发起。
     *
     * <p>为什么是 playTimeout*3：HTTP 接口的 DeferredResult 等待超时就是 playTimeout，
     * 兜底必须明显晚于它，避免用户还在等待时后台就提前释放认领、导致同一通道被重复 INVITE。
     * 也就是说，这个清理只处理“所有等待者都已经超时之后仍然没人调用 call()”的异常残留。</p>
     *
     * <p>并发边界：</p>
     * <ul>
     *     <li>ConcurrentHashMap 的迭代是弱一致的，迭代期间 call() 可能已经移除某个 Holder，
     *         因此用 {@code remove(key, holder)} 比较后删除，只有删除成功的一方才负责通知，
     *         避免 call() 和兜底清理对同一个 Holder 重复通知；</li>
     *     <li>释放认领后同一通道可能立刻有新的点播进来，回收旧状态前会先判断 InviteInfo
     *         的创建时间，避免误伤新会话。</li>
     * </ul>
     *
     * @param now 当前时间，参数化是为了便于单元测试，生产环境由定时任务传入
     */
    void cleanExpiredInviteCallbacks(long now) {
        if (inviteCallbackMap.isEmpty()) {
            return;
        }
        Integer playTimeout = userSetting.getPlayTimeout();
        long expireTime = (playTimeout == null ? 10000L : playTimeout) * 3L;
        for (Map.Entry<String, InviteCallbackHolder> entry : inviteCallbackMap.entrySet()) {
            InviteCallbackHolder holder = entry.getValue();
            long waited = now - holder.createTime;
            if (waited <= expireTime) {
                continue;
            }
            // 比较后删除：如果 call() 已经先一步移除了这个 Holder，这里会返回 false，
            // 说明结果已经通知过了，当前线程不再重复通知。
            if (!inviteCallbackMap.remove(entry.getKey(), holder)) {
                continue;
            }
            log.warn("[点播等待超时] 释放长时间未结束的点播请求: type={}, channelId={}, stream={}, 已等待={}ms",
                    holder.type, holder.channelId, holder.stream, waited);
            // 先回收旧会话状态，再通知等待者：
            // 等待者的回调里可能会立刻发起重试，如果先通知再回收，可能把重试产生的新会话状态误删。
            cleanupStaleInviteState(holder, now);
            notifyCallbacks(entry.getKey(), holder,
                    InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getCode(),
                    InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getMsg(), null);
        }
    }

    /**
     * 兜底释放认领后，尽量回收这次点播遗留的状态。
     *
     * <p>只清理“确认属于旧会话”的数据：</p>
     * <ul>
     *     <li>大多数情况下 InviteInfo 已经不存在（定时任务在 playTimeout*2 时已经清理过），
     *         此时直接跳过，不去动通道状态，避免误清同一通道上刚开始的新点播；</li>
     *     <li>如果 InviteInfo 的 createTime 不早于 now，说明释放认领之后已经有新请求接管，
     *         属于新会话，同样不能清理。</li>
     * </ul>
     *
     * <p>这里不主动关闭 RTP 端口/释放 SSRC：等到兜底执行时，InviteInfo 通常已经被清理，
     * 无法可靠拿到 mediaServerId/app/stream；而“看门狗注册失败”这个根因已经由 DynamicTask
     * 的修复解决。兜底只负责把卡死的认领和可确认的脏状态释放掉。</p>
     */
    private void cleanupStaleInviteState(InviteCallbackHolder holder, long now) {
        if (holder.channelId == null) {
            return;
        }
        try {
            InviteInfo inviteInfo = getInviteInfo(holder.type, holder.channelId, holder.stream);
            if (inviteInfo == null || inviteInfo.getCreateTime() == null
                    || inviteInfo.getCreateTime() >= now) {
                return;
            }
            removeInviteInfo(holder.type, holder.channelId, holder.stream);
            deviceChannelMapper.stopPlayById(holder.channelId);
            log.info("[点播等待超时] 已回收残留点播状态: type={}, channelId={}, stream={}",
                    holder.type, holder.channelId, holder.stream);
        } catch (Exception e) {
            log.error("[点播等待超时] 回收残留点播状态失败: type={}, channelId={}, stream={}",
                    holder.type, holder.channelId, holder.stream, e);
        }
    }

    @Override
    public InviteInfo getInviteInfoBySSRC(String ssrc) {
        List<InviteInfo> inviteInfoList = getAllInviteInfo();
        if (inviteInfoList.isEmpty()) {
            return null;
        }
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (inviteInfo.getSsrcInfo() != null && ssrc.equals(inviteInfo.getSsrcInfo().getSsrc())) {
                return inviteInfo;
            }
        }
        return null;
    }

    @Override
    public InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrc) {
        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInDb == null) {
            return null;
        }
        removeInviteInfo(inviteInfoInDb);
        String key = VideoManagerConstants.INVITE_PREFIX;
        String objectKey = inviteInfo.getType() +
                ":" + inviteInfo.getChannelId() +
                ":" + inviteInfo.getStream();
        if (inviteInfoInDb.getSsrcInfo() != null) {
            inviteInfoInDb.getSsrcInfo().setSsrc(ssrc);
        }
        redisTemplate.opsForHash().put(key, objectKey, inviteInfoInDb);
        return inviteInfoInDb;
    }

    @Scheduled(fixedRate = 10000)   //定时检测,清理错误的redis数据,防止因为错误数据导致的点播不可用
    public void execute(){
        // 先做内存认领的兜底清理，再做 Redis 数据清理：
        // 即使下面的 Redis 操作异常，也不会影响“卡死的点播认领被释放”这个最关键的动作。
        cleanExpiredInviteCallbacks(System.currentTimeMillis());
        String key = VideoManagerConstants.INVITE_PREFIX;
        if(redisTemplate.opsForHash().size(key) == 0) {
            return;
        }
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            try {
                InviteInfo inviteInfo = (InviteInfo)value;
                if (inviteInfo.getCleanTime() != null) {
                    // 带保留窗口的条目（录像下载）由 cleanTime 决定生命周期，未到期一律保留，
                    // 不能继续走下面“ready 阶段”的清理规则，否则会把正在使用的会话删掉。
                    cleanInviteInfoByCleanTime(inviteInfo);
                    continue;
                }
                if (inviteInfo.getStreamInfo() != null) {
                    continue;
                }
                if (inviteInfo.getCreateTime() == null || inviteInfo.getExpirationTime() == null) {
                    removeInviteInfo(inviteInfo);
                    continue;
                }
                long time = inviteInfo.getCreateTime() + inviteInfo.getExpirationTime();
                if (System.currentTimeMillis() > time) {
                    removeInviteInfo(inviteInfo);
                }
            } catch (Exception e) {
                log.error("[定时清理Invite] 处理异常，尝试删除该数据: {}", value, e);
                removeInviteInfoByValue(value);
            }
        }
    }

    /**
     * 按保留窗口回收条目。
     *
     * <p>只有设置了 {@link InviteInfo#getCleanTime()} 的条目会走到这里，目前是「流已离开等待写入下载地址」
     * 的录像下载会话。未到期时保留；到期后做<b>条件删除</b>：先按 {@code type:channelId:stream} 重新读取，
     * 只有 createTime 与快照一致时才删除，避免删掉同一 stream 上重新发起的新会话。</p>
     */
    private void cleanInviteInfoByCleanTime(InviteInfo inviteInfo) {
        Long cleanTime = inviteInfo.getCleanTime();
        if (cleanTime == null || System.currentTimeMillis() <= cleanTime) {
            return;
        }
        InviteInfo current = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (current == null || !Objects.equals(current.getCreateTime(), inviteInfo.getCreateTime())) {
            return;
        }
        if (current.getCleanTime() == null || System.currentTimeMillis() <= current.getCleanTime()) {
            // 期间被新一轮会话/延长保留刷新过，交给下一次检测
            return;
        }
        removeInviteInfo(current);
        log.info("[定时清理Invite] 回收录像下载会话: type={}, channelId={}, stream={}",
                current.getType(), current.getChannelId(), current.getStream());
    }

    private void removeInviteInfoByValue(Object value) {
        if (value instanceof InviteInfo) {
            removeInviteInfo((InviteInfo) value);
        } else {
            String key = VideoManagerConstants.INVITE_PREFIX;
            redisTemplate.opsForHash().entries(key).forEach((k, v) -> {
                if (v.equals(value)) {
                    redisTemplate.opsForHash().delete(key, k);
                }
            });
        }
    }

    @Override
    public boolean isStaleStreamDeparture(InviteInfo inviteInfo, Long streamCreateStamp) {
        if (inviteInfo == null || inviteInfo.getCreateTime() == null || streamCreateStamp == null || streamCreateStamp <= 0) {
            return false;
        }
        long streamCreateMs = streamCreateStamp * 1000L;
        // 时间戳与当前时间偏差过大(超过24小时)，认为数据异常，不做判断，保持原有清理逻辑
        if (Math.abs(streamCreateMs - System.currentTimeMillis()) > 24 * 3600 * 1000L) {
            return false;
        }
        // 流实例创建时间比当前会话创建时间至少早2秒(容忍ZLM与WVP的时钟偏差)，说明该注销事件属于旧会话
        return inviteInfo.getCreateTime() - streamCreateMs > 2000L;
    }
}
