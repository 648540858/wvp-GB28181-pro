package com.genersoft.iot.vmp.gb28181.event.offline;

import com.genersoft.iot.vmp.conf.UserSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;

/**    
 * @description:设备心跳超时监听,借助redis过期特性，进行监听，监听到说明设备心跳超时，发送离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:35:46     
 */
@Component
public class KeepaliveTimeoutListenerForPlatform extends KeyExpirationEventMessageListener {

    private Logger logger = LoggerFactory.getLogger(KeepaliveTimeoutListenerForPlatform.class);

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private UserSetup userSetup;

    public KeepaliveTimeoutListenerForPlatform(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }


    /**
     * 监听失效的key
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //  获取失效的key
        String expiredKey = message.toString();
        logger.debug(expiredKey);
        // 平台心跳到期,需要重发, 判断是否已经多次未收到心跳回复, 多次未收到,则重新发起注册, 注册尝试多次未得到回复,则认为平台离线
        String PLATFORM_KEEPLIVEKEY_PREFIX = VideoManagerConstants.PLATFORM_KEEPALIVE_PREFIX + userSetup.getServerId() + "_";
        String PLATFORM_REGISTER_PREFIX = VideoManagerConstants.PLATFORM_REGISTER_PREFIX + userSetup.getServerId() + "_";
        String KEEPLIVEKEY_PREFIX = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetup.getServerId() + "_";
        if (expiredKey.startsWith(PLATFORM_KEEPLIVEKEY_PREFIX)) {
            String platformGBId = expiredKey.substring(PLATFORM_KEEPLIVEKEY_PREFIX.length(),expiredKey.length());

            publisher.platformKeepaliveExpireEventPublish(platformGBId);
        }else if (expiredKey.startsWith(PLATFORM_REGISTER_PREFIX)) {
            String platformGBId = expiredKey.substring(PLATFORM_REGISTER_PREFIX.length(),expiredKey.length());

            publisher.platformNotRegisterEventPublish(platformGBId);
        }else{
            String deviceId = expiredKey.substring(KEEPLIVEKEY_PREFIX.length(),expiredKey.length());
            publisher.outlineEventPublish(deviceId, KEEPLIVEKEY_PREFIX);
        }

    }
}
