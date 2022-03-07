package com.genersoft.iot.vmp.gb28181.event.subscribe;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.RedisKeyExpirationEventMessageListener;
import com.genersoft.iot.vmp.conf.UserSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**    
 * 平台订阅到期事件
 */
@Component
public class SubscribeListenerForPlatform extends RedisKeyExpirationEventMessageListener {

    private Logger logger = LoggerFactory.getLogger(SubscribeListenerForPlatform.class);

	@Autowired
	private UserSetup userSetup;

    @Autowired
    private DynamicTask dynamicTask;

    public SubscribeListenerForPlatform(RedisMessageListenerContainer listenerContainer, UserSetup userSetup) {
        super(listenerContainer, userSetup);
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
        // 订阅到期
        String PLATFORM_KEEPLIVEKEY_PREFIX = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() + "_";
        if (expiredKey.startsWith(PLATFORM_KEEPLIVEKEY_PREFIX)) {
            // 取消定时任务
            dynamicTask.stop(expiredKey);
        }
    }
}
