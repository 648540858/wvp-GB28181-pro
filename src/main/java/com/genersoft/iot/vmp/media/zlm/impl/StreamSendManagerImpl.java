package com.genersoft.iot.vmp.media.zlm.impl;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.IStreamSendManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 管理流的发送
 */
@Component
public class StreamSendManagerImpl implements IStreamSendManager {

    private Logger logger = LoggerFactory.getLogger("StreamSendManagerImpl");

    private final static String datePrefix = "VMP_SEND_STREAM:DATA:";
    private final static String queryPrefix = "VMP_SEND_STREAM:QUERY:";

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void add(SendRtpItem sendRtpItem) {

    }

    @Override
    public void update(SendRtpItem sendRtpItem) {
        if (sendRtpItem.getId() != null) {
            sendRtpItem.setId(UUID.randomUUID().toString());
        }
        String dateId = datePrefix + sendRtpItem.getId();
        redisTemplate.opsForValue().set(datePrefix + sendRtpItem.getId(), sendRtpItem);

        if (sendRtpItem.getCallId() != null) {
            redisTemplate.opsForValue().set(getCallIdKey(sendRtpItem.getCallId()), dateId);
        }
        if (sendRtpItem.getApp() != null && sendRtpItem.getStreamId() != null) {
            redisTemplate.opsForSet().add(getAppAndStreamKey(sendRtpItem.getApp(), sendRtpItem.getStreamId()), dateId);
        }
        if (sendRtpItem.getMediaServerId() != null) {
            redisTemplate.opsForSet().add(getMediaServerIdKey(sendRtpItem.getMediaServerId()), dateId);
        }
        if (sendRtpItem.getDestId() != null) {
            redisTemplate.opsForSet().add(getDestIdKey(sendRtpItem.getDestId()), dateId);
        }
        if (sendRtpItem.getSourceId() != null) {
            redisTemplate.opsForSet().add(getSourceIdKey(sendRtpItem.getSourceId()), dateId);
        }
        if (sendRtpItem.getChannelId() != null) {
            redisTemplate.opsForSet().add(getChannelIdKey(sendRtpItem.getChannelId()), dateId);
        }
    }

    private String getCallIdKey(String callId) {
        return queryPrefix + "CALL_ID:" + callId;
    }

    private String getAppAndStreamKey(String app, String stream) {
        return queryPrefix + "APP_STREAM:" + app + "_" + stream;
    }

    private String getMediaServerIdKey(String mediaServerId) {
        return queryPrefix + "MEDIA_SERVER:" + mediaServerId;
    }

    private String getDestIdKey(String destId) {
        return queryPrefix + "DEST:" + destId;
    }

    private String getSourceIdKey(String sourceId) {
        return queryPrefix + "SOURCE:" + sourceId;
    }

    private String getChannelIdKey(String channelId) {
        return queryPrefix + "CHANNEL:" + channelId;
    }

    @Override
    public SendRtpItem getByCallId(String callId) {
        String dateId = (String) redisTemplate.opsForValue().get(getCallIdKey(callId));
        if (dateId == null) {
            return null;
        }
        return (SendRtpItem)redisTemplate.opsForValue().get(dateId);
    }


    @Override
    public List<SendRtpItem> getByAppAndStream(String app, String stream) {
        Set<Object> dateIds = redisTemplate.opsForSet().members(getAppAndStreamKey(app, stream));
        return getSendRtpItems(dateIds);
    }

    private List<SendRtpItem> getSendRtpItems(Set<Object> dateIds) {
        if (dateIds == null || dateIds.isEmpty()) {
            return null;
        }
        List<SendRtpItem> result = new ArrayList<>();
        for (Object dateId : dateIds) {
            result.add((SendRtpItem)redisTemplate.opsForValue().get(dateId));
        }
        return result;
    }

    @Override
    public List<SendRtpItem> getByMediaServerId(String mediaServerId) {
        Set<Object> dateIds = redisTemplate.opsForSet().members(getMediaServerIdKey(mediaServerId));
        return getSendRtpItems(dateIds);
    }

    @Override
    public List<SendRtpItem> getBySourceId(String sourceId) {
        Set<Object> dateIds = redisTemplate.opsForSet().members(getSourceIdKey(sourceId));
        return getSendRtpItems(dateIds);
    }

    @Override
    public List<SendRtpItem> getByDestId(String destId) {
        Set<Object> dateIds = redisTemplate.opsForSet().members(getDestIdKey(destId));
        return getSendRtpItems(dateIds);
    }

    @Override
    public List<SendRtpItem> getByByChanelId(String channelId) {
        Set<Object> dateIds = redisTemplate.opsForSet().members(getChannelIdKey(channelId));
        return getSendRtpItems(dateIds);
    }

    @Override
    public void remove(String id) {
        if (id == null) {
            return;
        }
        SendRtpItem sendRtpItem = (SendRtpItem) redisTemplate.opsForValue().get(datePrefix);
        if (sendRtpItem == null) {
            return;
        }

    }

    @Override
    public void remove(List<SendRtpItem> sendRtpItemList) {

    }
}
