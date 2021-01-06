package com.genersoft.iot.vmp.gb28181.event.offline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;

import java.nio.charset.StandardCharsets;

/**    
 * @Description:设备心跳超时监听,借助redis过期特性，进行监听，监听到说明设备心跳超时，发送离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:35:46     
 */
@Component
public class KeepaliveTimeoutListenerForPlatform extends KeyExpirationEventMessageListener {

	@Autowired
	private EventPublisher publisher;

    public KeepaliveTimeoutListenerForPlatform(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }


    /**
     * 监听失效的key
     * @param message
     * @param bytes
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //  获取失效的key
        String expiredKey = message.toString();
        System.out.println(expiredKey);
        if(!expiredKey.startsWith(VideoManagerConstants.PLATFORM_PREFIX)){
        	System.out.println("收到redis过期监听，但开头不是"+VideoManagerConstants.PLATFORM_PREFIX+"，忽略");
        	return;
        }
        // 平台心跳到期,需要重发, 判断是否已经多次未收到心跳回复, 多次未收到,则重新发起注册, 注册尝试多次未得到回复,则认为平台离线
        if (expiredKey.startsWith(VideoManagerConstants.PLATFORM_KEEPLIVEKEY_PREFIX)) {
            String platformGBId = expiredKey.substring(VideoManagerConstants.PLATFORM_KEEPLIVEKEY_PREFIX.length(),expiredKey.length());

            publisher.platformKeepaliveExpireEventPublish(platformGBId);
        }else if (expiredKey.startsWith(VideoManagerConstants.PLATFORM_REGISTER_PREFIX)) {
            System.out.println("11111111111111");
            String platformGBId = expiredKey.substring(VideoManagerConstants.PLATFORM_REGISTER_PREFIX.length(),expiredKey.length());

            publisher.platformNotRegisterEventPublish(platformGBId);
        }else{
            String deviceId = expiredKey.substring(VideoManagerConstants.KEEPLIVEKEY_PREFIX.length(),expiredKey.length());
            publisher.outlineEventPublish(deviceId, VideoManagerConstants.EVENT_OUTLINE_TIMEOUT);
        }

    }
}
