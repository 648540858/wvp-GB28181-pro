package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.service.impl.RedisAlarmMsgListener;
import com.genersoft.iot.vmp.service.impl.RedisGPSMsgListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.alibaba.fastjson.parser.ParserConfig;
import com.genersoft.iot.vmp.utils.redis.FastJsonRedisSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @description:Redis中间件配置类，使用spring-data-redis集成，自动从application.yml中加载redis配置
 * @author: swwheihei
 * @date: 2019年5月30日 上午10:58:25
 * 
 */
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

	@Value("${spring.redis.host}")
	private String host;
	@Value("${spring.redis.port}")
	private int port;
	@Value("${spring.redis.database}")
	private int database;
	@Value("${spring.redis.password}")
	private String password;
	@Value("${spring.redis.timeout}")
	private int timeout;
	@Value("${spring.redis.poolMaxTotal:1000}")
	private int poolMaxTotal;
	@Value("${spring.redis.poolMaxIdle:500}")
	private int poolMaxIdle;
	@Value("${spring.redis.poolMaxWait:5}")
	private int poolMaxWait;

	@Autowired
	private RedisGPSMsgListener redisGPSMsgListener;

	@Autowired
	private RedisAlarmMsgListener redisAlarmMsgListener;

	@Bean
	public JedisPool jedisPool() {
		if (StringUtils.isBlank(password)) {
			password = null;
		}
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(poolMaxIdle);
		poolConfig.setMaxTotal(poolMaxTotal);
		// 秒转毫秒
		poolConfig.setMaxWaitMillis(poolMaxWait * 1000L);
		JedisPool jp = new JedisPool(poolConfig, host, port, timeout * 1000, password, database);
		return jp;
	}

	@Bean("redisTemplate")
	public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		// 使用fastjson进行序列化处理，提高解析效率
		FastJsonRedisSerializer<Object> serializer = new FastJsonRedisSerializer<Object>(Object.class);
		// value值的序列化采用fastJsonRedisSerializer
		template.setValueSerializer(serializer);
		template.setHashValueSerializer(serializer);
		// key的序列化采用StringRedisSerializer
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory);
		// 使用fastjson时需设置此项，否则会报异常not support type
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
		return template;
	}

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
        return container;
    }

}
