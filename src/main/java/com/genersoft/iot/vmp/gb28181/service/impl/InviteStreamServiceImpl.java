package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
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

@Slf4j
@Service
public class InviteStreamServiceImpl implements IInviteStreamService {

    private final Map<String, List<ErrorCallback<StreamInfo>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema()) && "rtp".equals(event.getApp())) {
            InviteInfo inviteInfo = getInviteInfoByStream(null, event.getStream());
            if (inviteInfo != null && (inviteInfo.getType() == InviteSessionType.PLAY || inviteInfo.getType() == InviteSessionType.PLAYBACK)) {
                removeInviteInfo(inviteInfo);
                Device device = deviceMapper.getDeviceByDeviceId(inviteInfo.getDeviceId());
                if (device != null) {
                    deviceChannelMapper.stopPlayById(inviteInfo.getChannelId());
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
                InviteInfo inviteInfo = (InviteInfo) cursor.next().getValue();
                cursor.close();
                return inviteInfo;

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
            redisTemplate.opsForHash().delete(key);
            return;
        }
        InviteInfo inviteInfo = getInviteInfo(type, channelId, stream);
        if (inviteInfo != null) {
            String objectKey = inviteInfo.getType() +
                    ":" + inviteInfo.getChannelId() +
                    ":" + inviteInfo.getStream();
            redisTemplate.opsForHash().delete(key, objectKey);
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
        String key = buildSubStreamKey(type, channelId, stream);
        List<ErrorCallback<StreamInfo>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (ErrorCallback<StreamInfo> callback : callbacks) {
            if (callback != null) {
                callback.run(code, msg, data);
            }
        }
        inviteErrorCallbackMap.remove(key);
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
        String key = VideoManagerConstants.INVITE_PREFIX;
        if(redisTemplate.opsForHash().size(key) == 0) {
            return;
        }
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            InviteInfo inviteInfo = (InviteInfo)value;
            if (inviteInfo.getStreamInfo() != null) {
                continue;
            }
            if (inviteInfo.getCreateTime() == null || inviteInfo.getExpirationTime() == 0) {
                removeInviteInfo(inviteInfo);
            }
            long time = inviteInfo.getCreateTime() + inviteInfo.getExpirationTime();
            if (System.currentTimeMillis() > time) {
                removeInviteInfo(inviteInfo);
            }
        }
    }
}
