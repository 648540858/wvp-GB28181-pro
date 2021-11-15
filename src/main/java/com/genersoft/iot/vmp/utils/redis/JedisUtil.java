package com.genersoft.iot.vmp.utils.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

/**
 * @description:Jedis工具类
 * @author: wangshaopeng@sunnybs.com
 * @date: 2021年03月22日 下午8:27:29
 */
@Component
public class JedisUtil {

    @Autowired
    private JedisPool jedisPool;

    //    ============================== Key ==============================

    /**
     * 检查给定 key 是否存在。
     *
     * @param key
     * @return
     */
    public Boolean exists(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Boolean exists = jedis.exists(key);
            return exists;
        } finally {
            returnToPool(jedis);
        }
    }


    //    ============================== Set ==============================

    /**
     * SADD key member [member ...]
     * 将一个或多个 member 元素加入到集合 key 当中，已经存在于集合的 member 元素将被忽略。
     * 假如 key 不存在，则创建一个只包含 member 元素作成员的集合。
     * 当 key 不是集合类型时，返回一个错误。
     */
    public Long sadd(String key, String... members) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Long smove = jedis.sadd(key, members);
            return smove;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * SMEMBERS key
     * 返回集合 key 中的所有成员。
     * 不存在的 key 被视为空集合。
     */
    public Set<String> smembers(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Set<String> smembers = jedis.smembers(key);
            return smembers;
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * SREM key member1 [member2]
     * 移除集合中一个或多个成员
     */
    public Long srem(String key, String... member) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Long srem = jedis.srem(key, member);
            return srem;
        } finally {
            returnToPool(jedis);
        }
    }

    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}