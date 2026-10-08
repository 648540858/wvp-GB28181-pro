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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class InviteStreamServiceImpl implements IInviteStreamService {

    private final Map<String, List<ErrorCallback<StreamInfo>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    /**
     * 内存中点播等待回调的发起时间，用于兜底清理长时间未结束的点播请求，
     * 防止某条释放路径断裂后没有任何线程调用call()导致"已有请求在途"永久残留
     */
    private final Map<String, Long> inviteErrorCallbackCreateTimeMap = new ConcurrentHashMap<>();

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
            InviteInfo inviteInfo = getInviteInfoByStream(null, event.getStream());
            if (inviteInfo != null && (inviteInfo.getType() == InviteSessionType.PLAY || inviteInfo.getType() == InviteSessionType.PLAYBACK)) {
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
    public void removeInviteInfo(InviteSessionType type, Integer channelId, String stream) {
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
        String key = buildKey(type, channelId, stream);
        List<ErrorCallback<StreamInfo>> callbacks = inviteErrorCallbackMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);

    }

    @Override
    public boolean onceAndFirst(InviteSessionType type, Integer channelId, String stream, ErrorCallback<StreamInfo> callback) {
        String key = buildKey(type, channelId, stream);
        AtomicBoolean first = new AtomicBoolean(false);
        inviteErrorCallbackMap.computeIfAbsent(key, k -> {
            first.set(true);
            inviteErrorCallbackCreateTimeMap.put(k, System.currentTimeMillis());
            List<ErrorCallback<StreamInfo>> callbacks = new CopyOnWriteArrayList<>();
            callbacks.add(callback);
            return callbacks;
        });
        if (!first.get()) {
            List<ErrorCallback<StreamInfo>> callbacks = inviteErrorCallbackMap.get(key);
            if (callbacks != null) {
                callbacks.add(callback);
            }
        }
        return first.get();
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
        notifyAndRemove(buildSubStreamKey(type, channelId, stream), code, msg, data);
    }

    /**
     * 移除指定点播请求的等待回调，并通知所有等待者
     */
    private void notifyAndRemove(String key, int code, String msg, StreamInfo data) {
        inviteErrorCallbackCreateTimeMap.remove(key);
        List<ErrorCallback<StreamInfo>> callbacks = inviteErrorCallbackMap.remove(key);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (ErrorCallback<StreamInfo> callback : callbacks) {
            if (callback != null) {
                callback.run(code, msg, data);
            }
        }
    }

    /**
     * 兜底清理长时间未结束的点播等待回调，防止"已有请求在途"永久残留：
     * 正常流程下点播结束(成功/失败/超时)都会调用call()释放回调，若某条释放路径断裂
     * (例如收流超时看门狗因为任务key复用没能注册上)，就没有任何线程调用call()，
     * 点播认领会永久残留，该通道之后的点播一直返回"已有请求在途"。
     * 这里在超过 playTimeout*3 后释放认领并通知等待者，保证后续点播能够重新发起
     */
    private void cleanExpiredInviteCallbacks() {
        if (inviteErrorCallbackCreateTimeMap.isEmpty()) {
            return;
        }
        long expireTime = (long) userSetting.getPlayTimeout() * 3;
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : inviteErrorCallbackCreateTimeMap.entrySet()) {
            if (currentTime - entry.getValue() > expireTime) {
                log.warn("[点播等待超时] 释放长时间未结束的点播请求: {}", entry.getKey());
                notifyAndRemove(entry.getKey(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(),
                        InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
            }
        }
    }


    private String buildSubStreamKey(InviteSessionType type, Integer channelId, String stream) {
        String key = type + ":" + channelId;
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
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
        cleanExpiredInviteCallbacks();
        String key = VideoManagerConstants.INVITE_PREFIX;
        if(redisTemplate.opsForHash().size(key) == 0) {
            return;
        }
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            try {
                InviteInfo inviteInfo = (InviteInfo)value;
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
