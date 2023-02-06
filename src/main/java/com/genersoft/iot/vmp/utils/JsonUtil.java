package com.genersoft.iot.vmp.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

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
    public static <T> T redisJsonToObject(String key, Class<T> clazz) {
        Object jsonObject = RedisUtil.get(key);
        if (Objects.isNull(jsonObject)) {
            return null;
        }
        return clazz.cast(jsonObject);
    }
}