package com.genersoft.iot.vmp.gb28181.task.deviceStatus;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DeviceStatusTaskRunner {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private UserSetting userSetting;

    private final String prefix = "VMP_DEVICE_EXPIRES";
    private final String redisKey = String.format("%s_%s", prefix, userSetting.getServerId());

    // 状态过期检查
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    @Async
    public void expirationCheck(){

    }

    public void addTask(String deviceId, long expireTime) {
        redisTemplate.opsForZSet().add(redisKey, deviceId, expireTime);
    }

    public void removeTask(String deviceId) {
        redisTemplate.opsForZSet().remove(redisKey, deviceId);
    }

    public boolean containsKey(String deviceId) {
        if (ObjectUtils.isEmpty(deviceId)) {
            return false;
        }
        return redisTemplate.opsForZSet().score(redisKey, deviceId) != null;
    }

    public List<DeviceStatusTaskInfo> getAllTaskInfo(){
        String scanKey = String.format("%s_%s_*", prefix, userSetting.getServerId());
        List<Object> values = RedisUtil.scan(redisTemplate, scanKey);
        if (values.isEmpty()) {
            return new ArrayList<>();
        }
        List<DeviceStatusTaskInfo> result = new ArrayList<>();
        for (Object value : values) {
            String redisKey = (String)value;
            DeviceStatusTaskInfo taskInfo = (DeviceStatusTaskInfo)redisTemplate.opsForValue().get(redisKey);
            if (taskInfo == null) {
                continue;
            }
            Long expire = redisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
            taskInfo.setExpireTime(expire);
            result.add(taskInfo);
        }
        return result;

    }
}
