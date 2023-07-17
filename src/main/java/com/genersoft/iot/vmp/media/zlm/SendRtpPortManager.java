package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaSendRtpPortInfo;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String sendIndexKey = KEY + userSetting.getServerId() + "_" +  mediaServerId;
        MediaSendRtpPortInfo mediaSendRtpPortInfo = (MediaSendRtpPortInfo)redisTemplate.opsForValue().get(sendIndexKey);
        if (mediaSendRtpPortInfo == null) {
            logger.warn("[发送端口管理] 获取{}的发送端口时未找到端口信息", mediaServerId);
            return 0;
        }

        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*";
        List<Object> queryResult = RedisUtil.scan(redisTemplate, key);
        Map<Integer, SendRtpItem> sendRtpItemMap = new HashMap<>();

        for (Object o : queryResult) {
            SendRtpItem sendRtpItem = (SendRtpItem) redisTemplate.opsForValue().get(o);
            if (sendRtpItem != null) {
                sendRtpItemMap.put(sendRtpItem.getLocalPort(), sendRtpItem);
            }
        }

        int port = getPort(mediaSendRtpPortInfo.getCurrent(),
                mediaSendRtpPortInfo.getStart(),
                mediaSendRtpPortInfo.getEnd(), checkPort -> sendRtpItemMap.get(checkPort) == null);

        mediaSendRtpPortInfo.setCurrent(port);
        redisTemplate.opsForValue().set(sendIndexKey, mediaSendRtpPortInfo);
        return port;
    }

    interface CheckPortCallback{
        boolean check(int port);
    }

    private int getPort(int current, int start, int end, CheckPortCallback checkPortCallback) {
        int port;
        if (current %2 != 0) {
            port = current + 1;
        }else {
            port = current + 2;
        }
        if (port > end) {
            if (start %2 != 0) {
                port = start + 1;
            }else {
                port = start;
            }
        }
        if (!checkPortCallback.check(port)) {
            return getPort(port, start, end, checkPortCallback);
        }
        return port;
    }
}
