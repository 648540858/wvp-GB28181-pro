package com.genersoft.iot.vmp.conf;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.alibaba.fastjson.parser.ParserConfig;
import com.genersoft.iot.vmp.utils.redis.FastJsonRedisSerializer;

/**
 * @Description:Redis中间件配置类
 * @author: songww
 * @date: 2019年5月30日 上午10:58:25
 * 
 */
@Configuration
// @EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

	@Bean("redisTemplate")
	public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true); 
		FastJsonRedisSerializer<Object> serializer = new FastJsonRedisSerializer<Object>(Object.class);
		// value值的序列化采用fastJsonRedisSerializer
		template.setValueSerializer(serializer);
		template.setHashValueSerializer(serializer);
		// key的序列化采用StringRedisSerializer
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());

		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	/**
	 * redis消息监听器容器 可以添加多个监听不同话题的redis监听器，只需要把消息监听器和相应的消息订阅处理器绑定，该消息监听器
	 * 通过反射技术调用消息订阅处理器的相关方法进行一些业务处理
	 * 
	 * @param connectionFactory
	 * @param listenerAdapter
	 * @return
	 */
	@Bean
	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
//	@Bean
//	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
//			MessageListenerAdapter listenerAdapter) {
//
//		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//		container.setConnectionFactory(connectionFactory);
//		// 订阅了一个叫通道
//		container.addMessageListener(listenerAdapter, new PatternTopic(VideoManagerConstants.KEEPLIVEKEY_PREFIX+"*"));
//		// 这个container 可以添加多个 messageListener
//		return container;
//	}
	
//	/**
//     * 消息监听器适配器，绑定消息处理器，利用反射技术调用消息处理器的业务方法
//     * @param receiver
//     * @return
//     */
//    @Bean
//    MessageListenerAdapter listenerAdapter(MessageReceiver receiver) {
//        //这个地方 是给messageListenerAdapter 传入一个消息接受的处理器，利用反射的方法调用“receiveMessage”
//        //也有好几个重载方法，这边默认调用处理器的方法 叫handleMessage 可以自己到源码里面看
//        return new MessageListenerAdapter(receiver, "receiveMessage");
//    }
}
