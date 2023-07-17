package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.zlm.dto.MediaSendRtpPortInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SendRtpPortManager {

    private final static Logger logger = LoggerFactory.getLogger(SendRtpPortManager.class);

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private final String KEY = "VM_MEDIA_SEND_RTP_PORT_RANGE_";


    public void initServerPort(String mediaServerId, int startPort, int endPort){
        String key = KEY + userSetting.getServerId() + "_" +  mediaServerId;
        MediaSendRtpPortInfo mediaSendRtpPortInfo = new MediaSendRtpPortInfo(startPort, endPort, mediaServerId);
        redisTemplate.opsForValue().set(key, mediaSendRtpPortInfo);
    }

    public int getNextPort(String mediaServerId) {
        String key = KEY + userSetting.getServerId() + "_" +  mediaServerId;
        MediaSendRtpPortInfo mediaSendRtpPortInfo = (MediaSendRtpPortInfo)redisTemplate.opsForValue().get(key);
        if (mediaSendRtpPortInfo == null) {
            logger.warn("[发送端口管理] 获取{}的发送端口时未找到端口信息", mediaServerId);
            return 0;
        }
        int port;
        if (mediaSendRtpPortInfo.getCurrent() %2 != 0) {
            port = mediaSendRtpPortInfo.getCurrent() + 1;
        }else {
            port = mediaSendRtpPortInfo.getCurrent() + 2;
        }
        if (port > mediaSendRtpPortInfo.getEnd()) {
            if (mediaSendRtpPortInfo.getStart() %2 != 0) {
                port = mediaSendRtpPortInfo.getStart() + 1;
            }else {
                port = mediaSendRtpPortInfo.getStart();
            }
        }
        mediaSendRtpPortInfo.setCurrent(port);
        redisTemplate.opsForValue().set(key, mediaSendRtpPortInfo);
        return port;
    }
}
