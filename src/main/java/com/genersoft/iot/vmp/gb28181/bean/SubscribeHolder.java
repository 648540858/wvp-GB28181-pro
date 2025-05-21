package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
    private RedisTemplate<Object, Object> redisTemplate;

    private final String prefix = "VMP_SUBSCRIBE_OVERDUE";


    public void putCatalogSubscribe(String platformId, SubscribeInfo subscribeInfo) {
        log.info("[国标级联] 添加目录订阅，平台： {}， 有效期： {}", platformId, subscribeInfo.getExpires());
        if (subscribeInfo.getExpires() < 0) {
            return;
        }
        String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "catalog", platformId);
        Duration duration = Duration.ofSeconds(subscribeInfo.getExpires());
        redisTemplate.opsForValue().set(key, subscribeInfo, duration);
    }

    public SubscribeInfo getCatalogSubscribe(String platformId) {
        String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "catalog", platformId);
        return (SubscribeInfo)redisTemplate.opsForValue().get(key);
    }

    public void removeCatalogSubscribe(String platformId) {
        String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "catalog", platformId);
        redisTemplate.delete(key);
    }

    public void putMobilePositionSubscribe(String platformId, SubscribeInfo subscribeInfo, Runnable gpsTask) {
        log.info("[国标级联] 添加移动位置订阅，平台： {}， 有效期： {}", platformId, subscribeInfo.getExpires());
        if (subscribeInfo.getExpires() < 0) {
            return;
        }
        String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "mobilePosition", platformId);
        Duration duration = Duration.ofSeconds(subscribeInfo.getExpires());
        redisTemplate.opsForValue().set(key, subscribeInfo, duration);

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
        String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "mobilePosition", platformId);
        return (SubscribeInfo)redisTemplate.opsForValue().get(key);
    }

    public void removeMobilePositionSubscribe(String platformId) {
        String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "mobilePosition", platformId);
        redisTemplate.delete(key);
    }

    public List<String> getAllCatalogSubscribePlatform(List<Platform> platformList) {
        if (platformList == null || platformList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Platform platform : platformList) {
            String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "catalog", platform.getServerGBId());
            if (redisTemplate.hasKey(key)) {
                result.add(platform.getServerId());
            }
        }
        return result;
    }

    public List<String> getAllMobilePositionSubscribePlatform(List<Platform> platformList) {
        if (platformList == null || platformList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Platform platform : platformList) {
            String key = String.format("%s_%s_%s_%s", prefix, userSetting.getServerId(), "mobilePosition", platform.getServerGBId());
            if (redisTemplate.hasKey(key)) {
                result.add(platform.getServerId());
            }
        }
        return result;
    }
}
