package com.genersoft.iot.vmp.conf.redis;


import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.service.redisMsg.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;


/**
 * @description:Redis中间件配置类，使用spring-data-redis集成，自动从application.yml中加载redis配置
 * @author: swwheihei
 * @date: 2019年5月30日 上午10:58:25
 * 
 */
@Configuration
@Order(value=1)
public class RedisMsgListenConfig {

	@Autowired
	private RedisGpsMsgListener redisGPSMsgListener;

	@Autowired
	private RedisAlarmMsgListener redisAlarmMsgListener;

	@Autowired
	private RedisStreamMsgListener redisStreamMsgListener;

	@Autowired
	private RedisGbPlayMsgListener redisGbPlayMsgListener;

	@Autowired
	private RedisPushStreamStatusMsgListener redisPushStreamStatusMsgListener;

	@Autowired
	private RedisPushStreamStatusListMsgListener redisPushStreamListMsgListener;

	@Autowired
	private RedisPushStreamResponseListener redisPushStreamResponseListener;

	@Autowired
	private RedisCloseStreamMsgListener redisCloseStreamMsgListener;

	@Autowired
	private RedisPushStreamCloseResponseListener redisPushStreamCloseResponseListener;


	/**
	 * redis消息监听器容器 可以添加多个监听不同话题的redis监听器，只需要把消息监听器和相应的消息订阅处理器绑定，该消息监听器
	 * 通过反射技术调用消息订阅处理器的相关方法进行一些业务处理
	 * 
	 * @param connectionFactory
	 * @return
	 */
	@Bean
	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
		container.addMessageListener(redisGPSMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_GPS));
		container.addMessageListener(redisAlarmMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM_RECEIVE));
		container.addMessageListener(redisStreamMsgListener, new PatternTopic(VideoManagerConstants.WVP_MSG_STREAM_CHANGE_PREFIX + "PUSH"));
		container.addMessageListener(redisGbPlayMsgListener, new PatternTopic(RedisGbPlayMsgListener.WVP_PUSH_STREAM_KEY));
		container.addMessageListener(redisPushStreamStatusMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_PUSH_STREAM_STATUS_CHANGE));
		container.addMessageListener(redisPushStreamListMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_PUSH_STREAM_LIST_CHANGE));
		container.addMessageListener(redisPushStreamResponseListener, new PatternTopic(VideoManagerConstants.VM_MSG_STREAM_PUSH_RESPONSE));
		container.addMessageListener(redisCloseStreamMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE));
		container.addMessageListener(redisPushStreamCloseResponseListener, new PatternTopic(VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED));
        return container;
    }
}
