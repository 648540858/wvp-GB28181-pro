package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
@Slf4j
@Component
public class SubscribeHolder {

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final String prefix = "VMP_SUBSCRIBE_OVERDUE";

    public void putCatalogSubscribe(String platformId, SubscribeInfo subscribeInfo) {
        log.info("[国标级联] 添加目录订阅，平台： {}， 有效期： {}", platformId, subscribeInfo.getExpires());

        subscribeInfo.setServerId(userSetting.getServerId());
        String key = String.format("%s:%s:%s", prefix, "catalog", platformId);
        if (subscribeInfo.getExpires() > 0) {
            Duration duration = Duration.ofSeconds(subscribeInfo.getExpires());
            redisTemplate.opsForValue().set(key, subscribeInfo, duration);
        }else {
            redisTemplate.opsForValue().set(key, subscribeInfo);
        }
    }

    public SubscribeInfo getCatalogSubscribe(String platformId) {
        String key = String.format("%s:%s:%s", prefix, "catalog", platformId);
        return (SubscribeInfo)redisTemplate.opsForValue().get(key);
    }

    public void removeCatalogSubscribe(String platformId) {
        String key = String.format("%s:%s:%s", prefix, "catalog", platformId);
        redisTemplate.delete(key);
    }

    public void putMobilePositionSubscribe(String platformId, SubscribeInfo subscribeInfo, Runnable gpsTask) {
        log.info("[国标级联] 添加移动位置订阅，平台： {}， 有效期： {}s", platformId, subscribeInfo.getExpires());
        subscribeInfo.setServerId(userSetting.getServerId());
        String key = String.format("%s:%s:%s", prefix, "mobilePosition", platformId);
        if (subscribeInfo.getExpires() > 0) {
            Duration duration = Duration.ofSeconds(subscribeInfo.getExpires());
            redisTemplate.opsForValue().set(key, subscribeInfo, duration);
        }else {
            redisTemplate.opsForValue().set(key, subscribeInfo);
        }
        int cycleForCatalog;
        if (subscribeInfo.getGpsInterval() <= 0) {
            cycleForCatalog = 5;
        }else {
            cycleForCatalog = subscribeInfo.getGpsInterval();
        }
        dynamicTask.startCron(
                key,
                () -> {
                    SubscribeInfo subscribe = getMobilePositionSubscribe(platformId);
                    if (subscribe != null) {
                        gpsTask.run();
                    }else {
                        dynamicTask.stop(key);
                    }
                },
                cycleForCatalog * 1000);

    }

    public SubscribeInfo getMobilePositionSubscribe(String platformId) {
        String key = String.format("%s:%s:%s", prefix, "mobilePosition", platformId);
        return (SubscribeInfo)redisTemplate.opsForValue().get(key);
    }

    public void removeMobilePositionSubscribe(String platformId) {
        String key = String.format("%s:%s:%s", prefix, "mobilePosition", platformId);
        redisTemplate.delete(key);
    }

    public List<String> getAllCatalogSubscribePlatform(List<Platform> platformList) {
        if (platformList == null || platformList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Platform platform : platformList) {
            String key = String.format("%s:%s:%s", prefix, "catalog", platform.getServerGBId());
            if (redisTemplate.hasKey(key)) {
                result.add(platform.getServerGBId());
            }
        }
        return result;
    }

    public Map<Integer, Platform> getAllMobilePositionSubscribePlatform(List<Platform> platformList) {
        if (platformList == null || platformList.isEmpty()) {
            return new HashMap<>();
        }
        Map<Integer, Platform> result = new HashMap<>();

        // 1. 先批量构建所有 key
        List<String> keys = platformList.stream()
                .map(platform -> String.format("%s:%s:%s", prefix, "mobilePosition", platform.getServerGBId()))
                .toList();

        // 2. 批量查询 Redis 【关键：只发1次请求！】
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                // 注意：这里使用的是底层 connection 接口
                connection.keyCommands().exists(key.getBytes());
            }
            return null; // 流水线模式下必须返回 null
        });
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) instanceof Boolean exists && exists) {
                result.put(platformList.get(i).getId(), platformList.get(i));
            }
        }
        return result;
    }
}
