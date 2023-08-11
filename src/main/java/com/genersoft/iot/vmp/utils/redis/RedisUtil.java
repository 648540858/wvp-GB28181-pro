package com.genersoft.iot.vmp.utils.redis;

import com.google.common.collect.Lists;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        Set<String> resultKeys = (Set<String>) redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            ScanOptions scanOptions = ScanOptions.scanOptions().match("*" + query + "*").count(1000).build();
            Cursor<byte[]> scan = connection.scan(scanOptions);
            Set<String> keys = new HashSet<>();
            while (scan.hasNext()) {
                byte[] next = scan.next();
                keys.add(new String(next));
            }
            return keys;
        });

        return Lists.newArrayList(resultKeys);
    }
}



