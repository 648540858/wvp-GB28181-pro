package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@DS("master")
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
        }else {
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
            if (inviteInfo.getDeviceId() == null
                    || inviteInfo.getChannelId() == null
                    || inviteInfo.getType() == null
                    || inviteInfo.getStream() == null
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
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + inviteInfoForUpdate.getType() +
                ":" + inviteInfoForUpdate.getDeviceId() +
                ":" + inviteInfoForUpdate.getChannelId() +
                ":" + inviteInfoForUpdate.getStream()+
                ":" + inviteInfoForUpdate.getSsrcInfo().getSsrc();
        if (time != null && time > 0) {
            redisTemplate.opsForValue().set(key, inviteInfoForUpdate, time, TimeUnit.SECONDS);
        }else {
            redisTemplate.opsForValue().set(key, inviteInfoForUpdate);
        }
    }

    @Override
    public InviteInfo updateInviteInfoForStream(InviteInfo inviteInfo, String stream) {

        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInDb == null) {
            return null;
        }
        removeInviteInfo(inviteInfoInDb);
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + inviteInfo.getType() +
                ":" + inviteInfo.getDeviceId() +
                ":" + inviteInfo.getChannelId() +
                ":" + stream +
                ":" + inviteInfo.getSsrcInfo().getSsrc();
        inviteInfoInDb.setStream(stream);
        if (inviteInfoInDb.getSsrcInfo() != null) {
            inviteInfoInDb.getSsrcInfo().setStream(stream);
        }
        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            redisTemplate.opsForValue().set(key, inviteInfoInDb, userSetting.getPlayTimeout() * 2, TimeUnit.SECONDS);
        }else {
            redisTemplate.opsForValue().set(key, inviteInfoInDb);
        }

        return inviteInfoInDb;
    }

    @Override
    public InviteInfo getInviteInfo(InviteSessionType type, Integer channelId, String stream) {
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*")
                + ":*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.isEmpty()) {
            return null;
        }
        if (scanResult.size() != 1) {
            log.warn("[获取InviteInfo] 发现 key: {}存在多条", key);
        }

        return (InviteInfo) redisTemplate.opsForValue().get(scanResult.get(0));
    }

    @Override
    public List<InviteInfo> getAllInviteInfo(InviteSessionType type, Integer channelId, String stream) {
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*")
                + ":*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.isEmpty()) {
            return new ArrayList<>();
        }
        List<InviteInfo> result = new ArrayList<>();
        for (Object keyObj : scanResult) {
            result.add((InviteInfo) redisTemplate.opsForValue().get(keyObj));
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
    public void removeInviteInfo(InviteSessionType type,  Integer channelId, String stream) {
        String scanKey = VideoManagerConstants.INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*") +
                ":*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, scanKey);
        if (!scanResult.isEmpty()) {
            for (Object keyObj : scanResult) {
                String key = (String) keyObj;
                InviteInfo inviteInfo = (InviteInfo) redisTemplate.opsForValue().get(key);
                if (inviteInfo == null) {
                    continue;
                }
                redisTemplate.delete(key);
                inviteErrorCallbackMap.remove(buildKey(type,channelId, inviteInfo.getStream()));
            }
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
    public void once(InviteSessionType type,  Integer channelId, String stream, ErrorCallback<StreamInfo> callback) {
        String key = buildKey(type, channelId, stream);
        List<ErrorCallback<StreamInfo>> callbacks = inviteErrorCallbackMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);

    }

    private String buildKey(InviteSessionType type,  Integer channelId, String stream) {
        String key = type + ":" + channelId;
        // 如果ssrc未null那么可以实现一个通道只能一次操作，ssrc不为null则可以支持一个通道多次invite
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }


    @Override
    public void clearInviteInfo(String deviceId) {
        List<InviteInfo> inviteInfoList = getAllInviteInfo(null, null, null);
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (inviteInfo.getDeviceId().equals(deviceId)) {
                removeInviteInfo(inviteInfo);
            }
        }
    }

    @Override
    public int getStreamInfoCount(String mediaServerId) {
        int count = 0;
        String key = VideoManagerConstants.INVITE_PREFIX + ":*:*:*:*:*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.isEmpty()) {
            return 0;
        }else {
            for (Object keyObj : scanResult) {
                String keyStr = (String) keyObj;
                InviteInfo inviteInfo = (InviteInfo) redisTemplate.opsForValue().get(keyStr);
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
        }
        return count;
    }

    @Override
    public void call(InviteSessionType type, Integer channelId, String stream, int code, String msg, StreamInfo data) {
        String key = buildSubStreamKey(type, channelId, stream);
        List<ErrorCallback<StreamInfo>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null) {
            return;
        }
        for (ErrorCallback<StreamInfo> callback : callbacks) {
            callback.run(code, msg, data);
        }
        inviteErrorCallbackMap.remove(key);
    }


    private String buildSubStreamKey(InviteSessionType type, Integer channelId, String stream) {
        String key = type + ":" + channelId;
        // 如果ssrc为null那么可以实现一个通道只能一次操作，ssrc不为null则可以支持一个通道多次invite
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }

    @Override
    public InviteInfo getInviteInfoBySSRC(String ssrc) {
        String key = VideoManagerConstants.INVITE_PREFIX + ":*:*:*:*:" + ssrc;
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.size() != 1) {
            return null;
        }

        return (InviteInfo) redisTemplate.opsForValue().get(scanResult.get(0));
    }

    @Override
    public InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrc) {
        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInDb == null) {
            return null;
        }
        removeInviteInfo(inviteInfoInDb);
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + inviteInfo.getType() +
                ":" + inviteInfo.getDeviceId() +
                ":" + inviteInfo.getChannelId() +
                ":" + inviteInfo.getStream() +
                ":" + ssrc;
        if (inviteInfoInDb.getSsrcInfo() != null) {
            inviteInfoInDb.getSsrcInfo().setSsrc(ssrc);
        }
        redisTemplate.opsForValue().set(key, inviteInfoInDb);
        return inviteInfoInDb;
    }
}
