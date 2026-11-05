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

    /**
     * 根据 pattern 删除 key，使用 SCAN 分批遍历，避免 KEYS 命令阻塞 Redis
     * 注意：pattern 为完整的匹配表达式，不会再额外添加通配符
     *
     * @param redisTemplate redis 模板
     * @param pattern       匹配表达式，例如 VMP_DEVICE_REGISTER:*
     */
    public static void deleteByPattern(RedisTemplate redisTemplate, String pattern) {
        List<Object> keys = redisTemplate.scanKeys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
    }
}


