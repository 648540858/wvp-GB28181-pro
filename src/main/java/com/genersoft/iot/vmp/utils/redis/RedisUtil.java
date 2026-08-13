package com.genersoft.iot.vmp.utils.redis;

import com.genersoft.iot.vmp.conf.local.RedisTemplate;

import java.util.List;

/**
 * Redis工具类
 *
 * @author swwheihei
 * @date 2020年5月6日 下午8:27:29
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})
public class RedisUtil {

    /**
     * 模糊查询
     *
     * @param query 查询参数
     * @return
     */
    public static List<Object> scan(RedisTemplate redisTemplate, String query) {
        return redisTemplate.scanKeys(query);
    }
}


