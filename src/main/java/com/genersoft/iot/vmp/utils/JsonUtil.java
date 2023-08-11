package com.genersoft.iot.vmp.utils;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

/**
 * JsonUtil
 *
 * @author KunLong-Luo
 * @version 1.0.0
 * @since 2023/2/2 15:24
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /**
     * safe json type conversion
     *
     * @param key   redis key
     * @param clazz cast type
     * @param <T>
     * @return result type
     */
    public static <T> T redisJsonToObject(RedisTemplate<Object, Object> redisTemplate, String key, Class<T> clazz) {
        Object jsonObject = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(jsonObject)) {
            return null;
        }
        return clazz.cast(jsonObject);
    }
}