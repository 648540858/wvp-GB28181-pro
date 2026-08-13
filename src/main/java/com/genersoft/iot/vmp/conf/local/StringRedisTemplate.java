package com.genersoft.iot.vmp.conf.local;

/**
 * 本地 StringRedisTemplate 门面，泛型固定为 String/String，与 spring-data-redis 的 StringRedisTemplate 同名同用法。
 */
public class StringRedisTemplate extends RedisTemplate<String, String> {

    public StringRedisTemplate(InMemoryStore store, LocalMsgBus msgBus,
                               org.springframework.data.redis.core.RedisTemplate<String, String> delegate) {
        super(store, msgBus, delegate);
    }
}
