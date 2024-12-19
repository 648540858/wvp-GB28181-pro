package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class SendRtpServerServiceImpl implements ISendRtpServerService {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String requesterId,
                                         String deviceId, Integer channelId, Boolean isTcp, Boolean rtcp) {
        int localPort = getNextPort(mediaServer);
        if (localPort <= 0) {
            return null;
        }
        return SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, deviceId, null, channelId,
                isTcp, rtcp, userSetting.getServerId());
    }

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String platformId,
                                         String app, String stream, Integer channelId, Boolean tcp, Boolean rtcp){

        int localPort = getNextPort(mediaServer);
        if (localPort <= 0) {
            throw new PlayException(javax.sip.message.Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        SendRtpInfo sendRtpInfo = SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, null, platformId, channelId,
                tcp, rtcp, userSetting.getServerId());
        if (sendRtpInfo == null) {
            return null;
        }
        sendRtpInfo.setApp(app);
        sendRtpInfo.setStream(stream);
        return sendRtpInfo;
    }

    @Override
    public void update(SendRtpInfo sendRtpItem) {
        redisTemplate.opsForHash().put(VideoManagerConstants.SEND_RTP_INFO_CALLID, sendRtpItem.getCallId(), sendRtpItem);
        redisTemplate.opsForHash().put(VideoManagerConstants.SEND_RTP_INFO_STREAM + sendRtpItem.getStream(), sendRtpItem.getTargetId(), sendRtpItem);
        redisTemplate.opsForHash().put(VideoManagerConstants.SEND_RTP_INFO_CHANNEL + sendRtpItem.getChannelId(), sendRtpItem.getTargetId(), sendRtpItem);
    }

    @Override
    public SendRtpInfo queryByChannelId(Integer channelId, String targetId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CHANNEL + channelId;
        return JsonUtil.redisHashJsonToObject(redisTemplate, key, targetId, SendRtpInfo.class);
    }

    @Override
    public SendRtpInfo queryByCallId(String callId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID;
        return (SendRtpInfo)redisTemplate.opsForHash().get(key, callId);
    }

    @Override
    public SendRtpInfo queryByStream(String stream, String targetId) {
        String key = VideoManagerConstants.SEND_RTP_INFO_STREAM + stream;
        return JsonUtil.redisHashJsonToObject(redisTemplate, key, targetId, SendRtpInfo.class);
    }

    @Override
    public List<SendRtpInfo> queryByStream(String stream) {
        String key = VideoManagerConstants.SEND_RTP_INFO_STREAM + stream;
        List<Object> values = redisTemplate.opsForHash().values(key);
        List<SendRtpInfo> result= new ArrayList<>();
        for (Object o : values) {
            result.add((SendRtpInfo) o);
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
        redisTemplate.opsForHash().delete(VideoManagerConstants.SEND_RTP_INFO_CALLID, sendRtpInfo.getCallId());
        redisTemplate.opsForHash().delete(VideoManagerConstants.SEND_RTP_INFO_STREAM + sendRtpInfo.getStream(), sendRtpInfo.getTargetId());
        redisTemplate.opsForHash().delete(VideoManagerConstants.SEND_RTP_INFO_CHANNEL + sendRtpInfo.getChannelId(), sendRtpInfo.getTargetId());
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
        String key = VideoManagerConstants.SEND_RTP_INFO_CHANNEL + channelId;
        List<Object> values = redisTemplate.opsForHash().values(key);
        List<SendRtpInfo> result= new ArrayList<>();
        for (Object o : values) {
            result.add((SendRtpInfo) o);
        }
        return result;
    }

    @Override
    public List<SendRtpInfo> queryAll() {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID;
        List<Object> values = redisTemplate.opsForHash().values(key);
        List<SendRtpInfo> result= new ArrayList<>();
        for (Object o : values) {
            result.add((SendRtpInfo) o);
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

    private Set<Integer> getAllSendRtpPort() {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID;
        List<Object> values = redisTemplate.opsForHash().values(key);
        Set<Integer> result = new HashSet<>();
        for (Object value : values) {
            SendRtpInfo sendRtpInfo = (SendRtpInfo) value;
            result.add(sendRtpInfo.getPort());
        }
        return result;
    }


    @Override
    public synchronized int getNextPort(MediaServer mediaServer) {
        if (mediaServer == null) {
            log.warn("[发送端口管理] 参数错误，mediaServer为NULL");
            return -1;
        }
        String sendIndexKey = VideoManagerConstants.SEND_RTP_PORT + userSetting.getServerId() + ":" +  mediaServer.getId();
        Set<Integer> sendRtpSet = getAllSendRtpPort();
        String sendRtpPortRange = mediaServer.getSendRtpPortRange();
        int startPort;
        int endPort;
        if (sendRtpPortRange != null) {
            String[] portArray = sendRtpPortRange.split(",");
            if (portArray.length != 2 || !NumberUtils.isParsable(portArray[0]) || !NumberUtils.isParsable(portArray[1])) {
                log.warn("{}发送端口配置格式错误，自动使用50000-60000作为端口范围", mediaServer.getId());
                startPort = 50000;
                endPort = 60000;
            }else {
                if ( Integer.parseInt(portArray[1]) - Integer.parseInt(portArray[0]) < 1) {
                    log.warn("{}发送端口配置错误,结束端口至少比开始端口大一，自动使用50000-60000作为端口范围", mediaServer.getId());
                    startPort = 50000;
                    endPort = 60000;
                }else {
                    startPort = Integer.parseInt(portArray[0]);
                    endPort = Integer.parseInt(portArray[1]);
                }
            }
        }else {
            log.warn("{}未设置发送端口默认值，自动使用50000-60000作为端口范围", mediaServer.getId());
            startPort = 50000;
            endPort = 60000;
        }
        if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
            log.warn("{}获取redis连接信息失败", mediaServer.getId());
            return -1;
        }
        return getSendPort(startPort, endPort, sendIndexKey, sendRtpSet);
    }

    private synchronized int getSendPort(int startPort, int endPort, String sendIndexKey, Set<Integer> sendRtpPortSet){
        // TODO 这里改为只取偶数端口
        RedisAtomicInteger redisAtomicInteger = new RedisAtomicInteger(sendIndexKey , redisTemplate.getConnectionFactory());
        if (redisAtomicInteger.get() < startPort) {
            redisAtomicInteger.set(startPort);
            return startPort;
        }else {
            int port = redisAtomicInteger.getAndIncrement();
            if (port > endPort) {
                redisAtomicInteger.set(startPort);
                if (sendRtpPortSet.contains(startPort)) {
                    return getSendPort(startPort, endPort, sendIndexKey, sendRtpPortSet);
                }else {
                    return startPort;
                }
            }
            if (sendRtpPortSet.contains(port)) {
                return getSendPort(startPort, endPort, sendIndexKey, sendRtpPortSet);
            }else {
                return port;
            }
        }
    }

}
