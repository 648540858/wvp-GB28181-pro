package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InviteStreamServiceImpl implements IInviteStreamService {

    private final Logger logger = LoggerFactory.getLogger(InviteStreamServiceImpl.class);

    private final Map<String, List<ErrorCallback<Object>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void updateInviteInfo(InviteInfo inviteInfo) {
        if (inviteInfo == null || (inviteInfo.getDeviceId() == null || inviteInfo.getChannelId() == null)) {
            logger.warn("[更新Invite信息]，参数不全： {}", JSON.toJSON(inviteInfo));
            return;
        }
        InviteInfo inviteInfoForUpdate = null;

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
            InviteInfo inviteInfoInRedis = getInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(),
                    inviteInfo.getChannelId(), inviteInfo.getStream());
            if (inviteInfoInRedis == null) {
                logger.warn("[更新Invite信息]，未从缓存中读取到Invite信息： deviceId: {}, channel: {}, stream: {}",
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
        redisTemplate.opsForValue().set(key, inviteInfoForUpdate);
    }

    @Override
    public InviteInfo updateInviteInfoForStream(InviteInfo inviteInfo, String stream) {

        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
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
        redisTemplate.opsForValue().set(key, inviteInfoInDb);
        return inviteInfoInDb;
    }

    @Override
    public InviteInfo getInviteInfo(InviteSessionType type, String deviceId, String channelId, String stream) {
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (deviceId != null ? deviceId : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*")
                + ":*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.size() != 1) {
            return null;
        }

        return (InviteInfo) redisTemplate.opsForValue().get(scanResult.get(0));
    }

    @Override
    public InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type, String deviceId, String channelId) {
        return getInviteInfo(type, deviceId, channelId, null);
    }

    @Override
    public InviteInfo getInviteInfoByStream(InviteSessionType type, String stream) {
        return getInviteInfo(type, null, null, stream);
    }

    @Override
    public void removeInviteInfo(InviteSessionType type, String deviceId, String channelId, String stream) {
        String scanKey = VideoManagerConstants.INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (deviceId != null ? deviceId : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*") +
                ":*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, scanKey);
        if (scanResult.size() > 0) {
            for (Object keyObj : scanResult) {
                String key = (String) keyObj;
                InviteInfo inviteInfo = (InviteInfo) redisTemplate.opsForValue().get(key);
                if (inviteInfo == null) {
                    continue;
                }
                redisTemplate.delete(key);
                inviteErrorCallbackMap.remove(buildKey(type, deviceId, channelId, inviteInfo.getStream()));
            }
        }
    }

    @Override
    public void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, String deviceId, String channelId) {
        removeInviteInfo(inviteSessionType, deviceId, channelId, null);
    }

    @Override
    public void removeInviteInfo(InviteInfo inviteInfo) {
        removeInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
    }

    @Override
    public void once(InviteSessionType type, String deviceId, String channelId, String stream, ErrorCallback<Object> callback) {
        String key = buildKey(type, deviceId, channelId, stream);
        List<ErrorCallback<Object>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null) {
            callbacks = new CopyOnWriteArrayList<>();
            inviteErrorCallbackMap.put(key, callbacks);
        }
        callbacks.add(callback);

    }

    private String buildKey(InviteSessionType type, String deviceId, String channelId, String stream) {
        String key = type + ":" +  deviceId + ":" + channelId;
        // 如果ssrc未null那么可以实现一个通道只能一次操作，ssrc不为null则可以支持一个通道多次invite
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }


    @Override
    public void clearInviteInfo(String deviceId) {
        removeInviteInfo(null, deviceId, null, null);
    }

    @Override
    public int getStreamInfoCount(String mediaServerId) {
        int count = 0;
        String key = VideoManagerConstants.INVITE_PREFIX + ":*:*:*:*:*";
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.size() == 0) {
            return 0;
        }else {
            for (Object keyObj : scanResult) {
                String keyStr = (String) keyObj;
                InviteInfo inviteInfo = (InviteInfo) redisTemplate.opsForValue().get(keyStr);
                if (inviteInfo != null && inviteInfo.getStreamInfo() != null && inviteInfo.getStreamInfo().getMediaServerId().equals(mediaServerId)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void call(InviteSessionType type, String deviceId, String channelId, String stream, int code, String msg, Object data) {
        String key = buildSubStreamKey(type, deviceId, channelId, stream);
        List<ErrorCallback<Object>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null) {
            return;
        }
        for (ErrorCallback<Object> callback : callbacks) {
            callback.run(code, msg, data);
        }
        inviteErrorCallbackMap.remove(key);
    }


    private String buildSubStreamKey(InviteSessionType type, String deviceId, String channelId, String stream) {
        String key = type + ":" + ":" +  deviceId + ":" + channelId;
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
        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInDb == null) {
            return null;
        }
        removeInviteInfo(inviteInfoInDb);
        String key = VideoManagerConstants.INVITE_PREFIX +
                ":" + inviteInfo.getType() +
                ":" + inviteInfo.getDeviceId() +
                ":" + inviteInfo.getChannelId() +
                ":" + inviteInfo.getStream() +
                ":" + inviteInfo.getSsrcInfo().getSsrc();
        if (inviteInfoInDb.getSsrcInfo() != null) {
            inviteInfoInDb.getSsrcInfo().setSsrc(ssrc);
        }
        redisTemplate.opsForValue().set(key, inviteInfoInDb);
        return inviteInfoInDb;
    }
}
