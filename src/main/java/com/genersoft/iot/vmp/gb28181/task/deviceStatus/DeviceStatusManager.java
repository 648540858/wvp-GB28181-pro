package com.genersoft.iot.vmp.gb28181.task.deviceStatus;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.lang.Thread;

@Slf4j
@Component
public class DeviceStatusManager {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private EventPublisher eventPublisher;

    private final String prefix = "VMP_DEVICE_EXPIRES";

    public String redisKey(){
        return String.format("%s_%s", prefix, userSetting.getServerId());
    }

    /**
     *  状态过期检查, 每秒检查一次， 系统启动10秒后开始检查
     */
    @Scheduled(fixedDelay = 1, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void expirationCheck(){
        long now = System.currentTimeMillis();
        // 获取已过期的 deviceId (Score 介于 0 到 现在之间)
        Set<String> expiredIds = redisTemplate.opsForZSet().rangeByScore(redisKey(), 0, now);

        if (expiredIds != null && !expiredIds.isEmpty()) {
            redisTemplate.opsForZSet().remove(redisKey(), expiredIds.toArray());
            // 使用 JDK 21 虚拟线程异步分发事件
            Thread.startVirtualThread(() -> {
                // 获取详情后删除缓存
//                    Device device = redisCatchStorage.getDevice(deviceId);
//                    redisCatchStorage.removeDevice(deviceId);
                // 发送 Spring 异步事件
                eventPublisher.deviceOfflineEventPublish(expiredIds);
            });

        }
    }

    public void add(String deviceId, long expireTime) {
        redisTemplate.opsForZSet().add(redisKey(), deviceId, expireTime);
    }

    public void remove(String deviceId) {
        redisTemplate.opsForZSet().remove(redisKey(), deviceId);
    }

    public boolean contains(String deviceId) {
        if (ObjectUtils.isEmpty(deviceId)) {
            return false;
        }
        return redisTemplate.opsForZSet().score(redisKey(), deviceId) != null;
    }

    public void clear() {
        redisTemplate.opsForZSet().removeRangeByScore(redisKey(), 0, Long.MAX_VALUE);
    }

    public Set<String> getAll() {
        return redisTemplate.opsForZSet().rangeByScore(redisKey(), 0, Long.MAX_VALUE);
    }
}
