package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
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

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, int port, String ssrc, String requesterId,
                                         String deviceId, Integer channelId, boolean isTcp, boolean rtcp) {
        int localPort = sendRtpPortManager.getNextPort(mediaServer);
        if (localPort == 0) {
            return null;
        }
        return SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, deviceId, null, channelId,
                isTcp, rtcp, userSetting.getServerId());
    }

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, int port, String ssrc, String platformId,
                                         String app, String stream, Integer channelId, boolean tcp, boolean rtcp){

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
        redisTemplate.opsForValue().set(VideoManagerConstants.SEND_RTP_INFO_STREAM + sendRtpItem.getStream(), sendRtpItem);
        redisTemplate.opsForValue().set(VideoManagerConstants.SEND_RTP_INFO_CHANNEL + sendRtpItem.getChannelId(), sendRtpItem);
    }

    @Override
    public SendRtpInfo queryByChannelId(Integer channelId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CHANNEL + channelId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public SendRtpInfo queryByCallId(String callId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID + callId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public SendRtpInfo queryByStream(String stream, String targetId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_STREAM + stream;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
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
        redisTemplate.delete(VideoManagerConstants.SEND_RTP_INFO_STREAM + sendRtpInfo.getStream());
        redisTemplate.delete(VideoManagerConstants.SEND_RTP_INFO_CHANNEL + sendRtpInfo.getChannelId());
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
    public void deleteByStream(String Stream) {
        SendRtpInfo sendRtpInfo = queryByStream(Stream);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }
    @Override
    public void deleteByChannel(Integer channelId) {
        SendRtpInfo sendRtpInfo = queryByChannelId(channelId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
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
        SendRtpInfo sendRtpInfo = queryByChannelId(channelId);
        return sendRtpInfo != null;
    }

}
