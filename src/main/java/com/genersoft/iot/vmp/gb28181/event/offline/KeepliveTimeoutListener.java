package com.genersoft.iot.vmp.gb28181.event.offline;

import com.genersoft.iot.vmp.conf.RedisKeyExpirationEventMessageListener;
import com.genersoft.iot.vmp.conf.UserSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
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
public class KeepliveTimeoutListener extends RedisKeyExpirationEventMessageListener {

    private Logger logger = LoggerFactory.getLogger(KeepliveTimeoutListener.class);

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private UserSetting userSetting;

    public KeepliveTimeoutListener(RedisMessageListenerContainer listenerContainer, UserSetting userSetting) {
        super(listenerContainer, userSetting);
    }

    @Override
    public void init() {
        if (!userSetting.getRedisConfig()) {
            // 配置springboot默认Config为空，即不让应用去修改redis的默认配置，因为Redis服务出于安全会禁用CONFIG命令给远程用户使用
            setKeyspaceNotificationsConfigParameter("");
        }
        super.init();
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
        String KEEPLIVEKEY_PREFIX = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetting.getServerId() + "_";
        if(!expiredKey.startsWith(KEEPLIVEKEY_PREFIX)){
        	logger.debug("收到redis过期监听，但开头不是"+KEEPLIVEKEY_PREFIX+"，忽略");
        	return;
        }
        
        String deviceId = expiredKey.substring(KEEPLIVEKEY_PREFIX.length(),expiredKey.length());
        publisher.outlineEventPublish(deviceId, VideoManagerConstants.EVENT_OUTLINE_TIMEOUT);
    }
}
