package com.genersoft.iot.vmp.conf.local;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.service.redisMsg.RedisAlarmMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisCloseStreamMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisGpsMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisGroupChangeListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisGroupMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamListMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamResponseListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamStatusMsgListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 本地 Redis 门面配置：
 * <ul>
 *   <li>redis 模式（配置了 spring.data.redis.host）：门面委托给 spring-data-redis 真实实现，行为与旧版一致；</li>
 *   <li>内存模式（未配置）：使用 InMemoryStore + LocalMsgBus，不依赖 Redis 进程。</li>
 * </ul>
 */
@Configuration
public class LocalRedisConfig {

    @Value("${spring.data.redis.host:}")
    private String redisHost;

    private boolean isRedisMode() {
        return redisHost != null && !redisHost.isBlank();
    }

    /**
     * 内存模式下把业务监听器挂到本地消息总线；redis 模式下仍由 RedisMsgListenConfig 的容器订阅。
     * 使用 @Bean 参数注入，避免配置类自引用导致的循环依赖。
     */
    @Bean
    public Object localMsgBusSubscriber(LocalMsgBus msgBus,
                                        RedisGpsMsgListener redisGPSMsgListener,
                                        RedisAlarmMsgListener redisAlarmMsgListener,
                                        RedisPushStreamStatusMsgListener redisPushStreamStatusMsgListener,
                                        RedisPushStreamListMsgListener pushStreamListMsgListener,
                                        RedisCloseStreamMsgListener redisCloseStreamMsgListener,
                                        RedisPushStreamResponseListener redisPushStreamCloseResponseListener,
                                        RedisGroupMsgListener groupMsgListener,
                                        RedisGroupChangeListener groupChangeListener) {
        if (isRedisMode()) {
            return new Object();
        }
        msgBus.subscribe(VideoManagerConstants.VM_MSG_GPS, redisGPSMsgListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM_RECEIVE, redisAlarmMsgListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_PUSH_STREAM_STATUS_CHANGE, redisPushStreamStatusMsgListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_PUSH_STREAM_LIST_CHANGE, pushStreamListMsgListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE, redisCloseStreamMsgListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_STREAM_PUSH_RESPONSE, redisPushStreamCloseResponseListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_GROUP_LIST_RESPONSE, groupMsgListener);
        msgBus.subscribe(VideoManagerConstants.VM_MSG_GROUP_LIST_CHANGE, groupChangeListener);
        return new Object();
    }

    @Bean
    public InMemoryStore inMemoryStore() {
        return new InMemoryStore();
    }

    @Bean
    public LocalMsgBus localMsgBus() {
        return new LocalMsgBus();
    }

    @Bean
    public RedisTemplate<String, Object> localRedisTemplate(
            InMemoryStore store,
            LocalMsgBus msgBus,
            ObjectProvider<org.springframework.data.redis.core.RedisTemplate<String, Object>> redisTemplateProvider) {
        return new RedisTemplate<>(store, msgBus, isRedisMode() ? redisTemplateProvider.getIfAvailable() : null);
    }

    @Bean
    public StringRedisTemplate localStringRedisTemplate(
            InMemoryStore store,
            LocalMsgBus msgBus,
            ObjectProvider<org.springframework.data.redis.core.RedisTemplate<String, String>> stringRedisTemplateProvider) {
        return new StringRedisTemplate(store, msgBus, isRedisMode() ? stringRedisTemplateProvider.getIfAvailable() : null);
    }

    @Bean
    public RedisTemplate<String, Long> localRedisLongTemplate(
            InMemoryStore store,
            LocalMsgBus msgBus,
            ObjectProvider<org.springframework.data.redis.core.RedisTemplate<String, Long>> redisLongTemplateProvider) {
        return new RedisTemplate<>(store, msgBus, isRedisMode() ? redisLongTemplateProvider.getIfAvailable() : null);
    }

    @Scheduled(fixedRate = 60000)
    public void purgeExpiredKeys() {
        inMemoryStore().purgeExpired();
    }
}
