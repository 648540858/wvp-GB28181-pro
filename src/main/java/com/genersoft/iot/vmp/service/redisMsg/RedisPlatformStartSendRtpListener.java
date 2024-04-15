package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 收到消息后开始给上级发流
 * @author lin
 */
@Component
public class RedisPlatformStartSendRtpListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisPlatformStartSendRtpListener.class);

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.info("[REDIS消息-收到上级等到设备推流的redis消息]： {}", new String(message.getBody()));
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        SendRtpItem sendRtpItem = JSON.parseObject(new String(msg.getBody()), SendRtpItem.class);
                        sendRtpItem.getMediaServerId();
                        MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                        if (mediaServer == null) {
                            return;
                        }
                        Map<String, Object> sendRtpParam = getSendRtpParam(sendRtpItem);
                        sendRtp(sendRtpItem, mediaServer, sendRtpParam);

                    }catch (Exception e) {
                        logger.warn("[REDIS消息-请求推流结果] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[REDIS消息-请求推流结果] 异常内容： ", e);
                    }
                }
            });
        }
    }

    private Map<String, Object> getSendRtpParam(SendRtpItem sendRtpItem) {
        String isUdp = sendRtpItem.isTcp() ? "0" : "1";
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app",sendRtpItem.getApp());
        param.put("stream",sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("dst_url",sendRtpItem.getIp());
        param.put("dst_port", sendRtpItem.getPort());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", isUdp);
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
        }
        return param;
    }

    private JSONObject sendRtp(SendRtpItem sendRtpItem, MediaServerItem mediaInfo, Map<String, Object> param){
        JSONObject startSendRtpStreamResult = null;
        if (sendRtpItem.getLocalPort() != 0) {
            if (sendRtpItem.isTcpActive()) {
                startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                startSendRtpStreamResult = zlmServerFactory.startSendRtpStream(mediaInfo, param);
            }
        }else {
            if (sendRtpItem.isTcpActive()) {
                startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                startSendRtpStreamResult = zlmServerFactory.startSendRtpStream(mediaInfo, param);
            }
        }
        return startSendRtpStreamResult;

    }
}
