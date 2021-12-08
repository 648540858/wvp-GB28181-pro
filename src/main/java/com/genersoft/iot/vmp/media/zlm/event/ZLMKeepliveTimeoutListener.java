package com.genersoft.iot.vmp.media.zlm.event;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**    
 * @description:设备心跳超时监听,借助redis过期特性，进行监听，监听到说明设备心跳超时，发送离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:35:46     
 */
@Component
public class ZLMKeepliveTimeoutListener extends KeyExpirationEventMessageListener {

    private Logger logger = LoggerFactory.getLogger(ZLMKeepliveTimeoutListener.class);

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private UserSetup userSetup;

	public ZLMKeepliveTimeoutListener(RedisMessageListenerContainer listenerContainer) {
		super(listenerContainer);
        // 配置springboot默认Config为空，即不让应用去修改redis的默认配置，因为Redis服务出于安全会禁用CONFIG命令给远程用户使用
        setKeyspaceNotificationsConfigParameter("");
	}

	/**
     * 监听失效的key，key格式为keeplive_deviceId
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //  获取失效的key
        String expiredKey = message.toString();
        String KEEPLIVEKEY_PREFIX = VideoManagerConstants.MEDIA_SERVER_KEEPALIVE_PREFIX + userSetup.getServerId() + "_";
        if(!expiredKey.startsWith(KEEPLIVEKEY_PREFIX)){
        	return;
        }
        
        String mediaServerId = expiredKey.substring(KEEPLIVEKEY_PREFIX.length(),expiredKey.length());

        publisher.zlmOfflineEventPublish(mediaServerId);
    }
}
