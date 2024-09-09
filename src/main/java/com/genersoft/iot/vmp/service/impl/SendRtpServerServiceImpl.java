package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.conf.StackLoggerImpl;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SendRtpServerServiceImpl implements ISendRtpServerService {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private StackLoggerImpl stackLoggerImpl;

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String requesterId,
                                         String deviceId, Integer channelId, Boolean isTcp, Boolean rtcp) {
        int localPort = sendRtpPortManager.getNextPort(mediaServer);
        if (localPort == 0) {
            return null;
        }
        return SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, deviceId, null, channelId,
                isTcp, rtcp, userSetting.getServerId());
    }

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String platformId,
                                         String app, String stream, Integer channelId, Boolean tcp, Boolean rtcp){

        int localPort = sendRtpPortManager.getNextPort(mediaServer);
        if (localPort <= 0) {
            throw new PlayException(javax.sip.message.Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        return SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, null, platformId, channelId,
                tcp, rtcp, userSetting.getServerId());
    }

    @Override
    public void update(SendRtpInfo sendRtpItem) {
        redisTemplate.opsForValue().set(VideoManagerConstants.SEND_RTP_INFO_CALLID + sendRtpItem.getCallId(), sendRtpItem);
        redisTemplate.opsForValue().set(VideoManagerConstants.SEND_RTP_INFO_STREAM + sendRtpItem.getStream() + ":" + sendRtpItem.getTargetId(), sendRtpItem);
        redisTemplate.opsForValue().set(VideoManagerConstants.SEND_RTP_INFO_CHANNEL + sendRtpItem.getChannelId() + ":" + sendRtpItem.getTargetId(), sendRtpItem);
    }

    @Override
    public SendRtpInfo queryByChannelId(Integer channelId, String targetId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CHANNEL + channelId + ":" + targetId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public SendRtpInfo queryByCallId(String callId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID + callId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public SendRtpInfo queryByStream(String stream, String targetId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_STREAM + stream + ":" + targetId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public List<SendRtpInfo> queryByStream(String stream) {
        String key = VideoManagerConstants.SEND_RTP_INFO_STREAM + stream + ":*";
        List<Object> queryResult = RedisUtil.scan(redisTemplate, key);
        List<SendRtpInfo> result= new ArrayList<>();

        for (Object o : queryResult) {
            String keyItem = (String) o;
            result.add((SendRtpInfo) redisTemplate.opsForValue().get(keyItem));
        }

        return result;
    }

    /**
     * 删除RTP推送信息缓存
     */
    @Override
    public void delete(SendRtpInfo sendRtpInfo) {
        if (sendRtpInfo == null) {
            return;
        }
        redisTemplate.delete(VideoManagerConstants.SEND_RTP_INFO_CALLID + sendRtpInfo.getCallId());
        redisTemplate.delete(VideoManagerConstants.SEND_RTP_INFO_STREAM + sendRtpInfo.getStream() + ":" + sendRtpInfo.getTargetId());
        redisTemplate.delete(VideoManagerConstants.SEND_RTP_INFO_CHANNEL + sendRtpInfo.getChannelId() + ":" + sendRtpInfo.getTargetId());
    }
    @Override
    public void deleteByCallId(String callId) {
        SendRtpInfo sendRtpInfo = queryByCallId(callId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }
    @Override
    public void deleteByStream(String stream, String targetId) {
        SendRtpInfo sendRtpInfo = queryByStream(stream, targetId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }

    @Override
    public void deleteByStream(String stream) {
        List<SendRtpInfo> sendRtpInfos = queryByStream(stream);
        for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
            delete(sendRtpInfo);
        }
    }

    @Override
    public void deleteByChannel(Integer channelId, String targetId) {
        SendRtpInfo sendRtpInfo = queryByChannelId(channelId, targetId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }

    @Override
    public List<SendRtpInfo> queryByChannelId(int channelId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CHANNEL + channelId + ":*";
        List<Object> queryResult = RedisUtil.scan(redisTemplate, key);
        List<SendRtpInfo> result= new ArrayList<>();

        for (Object o : queryResult) {
            String keyItem = (String) o;
            result.add((SendRtpInfo) redisTemplate.opsForValue().get(keyItem));
        }

        return result;
    }

    @Override
    public List<SendRtpInfo> queryAll() {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID
                + userSetting.getServerId() + ":*";
        List<Object> queryResult = RedisUtil.scan(redisTemplate, key);
        List<SendRtpInfo> result= new ArrayList<>();

        for (Object o : queryResult) {
            String keyItem = (String) o;
            result.add((SendRtpInfo) redisTemplate.opsForValue().get(keyItem));
        }

        return result;
    }

    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     */
    @Override
    public boolean isChannelSendingRTP(Integer channelId) {
        List<SendRtpInfo> sendRtpInfoList = queryByChannelId(channelId);
        return !sendRtpInfoList.isEmpty();
    }

    @Override
    public List<SendRtpInfo> queryForPlatform(String platformId) {
        List<SendRtpInfo> sendRtpInfos = queryAll();
        if (!sendRtpInfos.isEmpty()) {
            sendRtpInfos.removeIf(sendRtpInfo -> !sendRtpInfo.isSendToPlatform() || !sendRtpInfo.getTargetId().equals(platformId));
        }
        return sendRtpInfos;
    }
}
