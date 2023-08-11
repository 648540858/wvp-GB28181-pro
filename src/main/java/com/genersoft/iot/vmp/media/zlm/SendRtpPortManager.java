package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
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

    private final String KEY = "VM_MEDIA_SEND_RTP_PORT_";

    public synchronized int getNextPort(MediaServerItem mediaServer) {
        if (mediaServer == null) {
            logger.warn("[发送端口管理] 参数错误，mediaServer为NULL");
            return -1;
        }
        String sendIndexKey = KEY + userSetting.getServerId() + "_" +  mediaServer.getId();
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
        String sendRtpPortRange = mediaServer.getSendRtpPortRange();
        int startPort;
        int endPort;
        if (sendRtpPortRange != null) {
            String[] portArray = sendRtpPortRange.split(",");
            if (portArray.length != 2 || !NumberUtils.isParsable(portArray[0]) || !NumberUtils.isParsable(portArray[1])) {
                logger.warn("{}发送端口配置格式错误，自动使用50000-60000作为端口范围", mediaServer.getId());
                startPort = 50000;
                endPort = 60000;
            }else {
                if ( Integer.parseInt(portArray[1]) - Integer.parseInt(portArray[0]) < 1) {
                    logger.warn("{}发送端口配置错误,结束端口至少比开始端口大一，自动使用50000-60000作为端口范围", mediaServer.getId());
                    startPort = 50000;
                    endPort = 60000;
                }else {
                    startPort = Integer.parseInt(portArray[0]);
                    endPort = Integer.parseInt(portArray[1]);
                }
            }
        }else {
            logger.warn("{}未设置发送端口默认值，自动使用50000-60000作为端口范围", mediaServer.getId());
            startPort = 50000;
            endPort = 60000;
        }
        if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
            logger.warn("{}获取redis连接信息失败", mediaServer.getId());
            return -1;
        }
//        RedisAtomicInteger redisAtomicInteger = new RedisAtomicInteger(sendIndexKey , redisTemplate.getConnectionFactory());
//        return redisAtomicInteger.getAndUpdate((current)->{
//            return getPort(current, startPort, endPort, checkPort-> !sendRtpItemMap.containsKey(checkPort));
//        });
        return getSendPort(startPort, endPort, sendIndexKey, sendRtpItemMap);
    }

    private synchronized int getSendPort(int startPort, int endPort, String sendIndexKey, Map<Integer, SendRtpItem> sendRtpItemMap){
        RedisAtomicInteger redisAtomicInteger = new RedisAtomicInteger(sendIndexKey , redisTemplate.getConnectionFactory());
        if (redisAtomicInteger.get() < startPort) {
            redisAtomicInteger.set(startPort);
            return startPort;
        }else {
            int port = redisAtomicInteger.getAndIncrement();
            if (port > endPort) {
                redisAtomicInteger.set(startPort);
                if (sendRtpItemMap.containsKey(startPort)) {
                    return getSendPort(startPort, endPort, sendIndexKey, sendRtpItemMap);
                }else {
                    return startPort;
                }
            }
            if (sendRtpItemMap.containsKey(port)) {
                return getSendPort(startPort, endPort, sendIndexKey, sendRtpItemMap);
            }else {
                return port;
            }
        }

    }

    interface CheckPortCallback{
        boolean check(int port);
    }

    private int getPort(int current, int start, int end, CheckPortCallback checkPortCallback) {
        if (current <= 0) {
            if (start%2 == 0) {
                current = start;
            }else {
                current = start + 1;
            }
        }else {
            current += 2;
            if (current > end) {
                if (start%2 == 0) {
                    current = start;
                }else {
                    current = start + 1;
                }
            }
        }
        if (!checkPortCallback.check(current)) {
            return getPort(current + 2, start, end, checkPortCallback);
        }
        return current;
    }
}
