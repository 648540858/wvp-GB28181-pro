package com.genersoft.iot.vmp.gb28181.task.deviceSubscribe;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SubscribeTaskRunner{

    private final Map<String, SubscribeTask> subscribes = new ConcurrentHashMap<>();

    private final DelayQueue<SubscribeTask> delayQueue = new DelayQueue<>();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserSetting userSetting;

    private final String prefix = "VMP_DEVICE_SUBSCRIBE";

    // 订阅过期检查
    @Scheduled(fixedDelay = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void expirationCheck(){
        SubscribeTask take;
        while ((take = delayQueue.poll()) != null) {
            try {
                // 出队后、清理前可能被 updateDelay 续期，续期后的任务不属于本次到期
                if (take.getDelay(TimeUnit.MILLISECONDS) > 0) {
                    continue;
                }
                // 同 key 可能已注册新任务，或被显式移除；仅清理本次出队到的任务实例
                if (!removeSubscribe(take.getKey(), take)) {
                    continue;
                }
                take.expired();
            } catch (Exception e) {
                log.error("[设备订阅到期] {} 到期处理时出现异常， 设备编号: {} ", take.getName(), take.getDeviceId());
            }
        }
    }

    public void addSubscribe(SubscribeTask task) {
        Duration duration = Duration.ofSeconds((task.getDelayTime() - System.currentTimeMillis())/1000);
        if (duration.getSeconds() < 0) {
            return;
        }
        subscribes.put(task.getKey(), task);
        String key = String.format("%s_%s_%s", prefix, userSetting.getServerId(), task.getKey());
        redisTemplate.opsForValue().set(key, task.getInfo(), duration);
        delayQueue.offer(task);
    }

    public boolean removeSubscribe(String key) {
        SubscribeTask task = subscribes.get(key);
        if (task == null) {
            return false;
        }
        deleteSubscribeRedisKey(key);
        subscribes.remove(key);
        if (delayQueue.contains(task)) {
            boolean remove = delayQueue.remove(task);
            if (!remove) {
                log.info("[移除订阅任务] 从延时队列内移除失败： {}", key);
            }
        }
        return true;
    }

    /**
     * 仅当 map 中仍是本次到期任务实例时清理，避免误删同 key 的新任务
     */
    private boolean removeSubscribe(String key, SubscribeTask expected) {
        if (expected == null || !subscribes.remove(key, expected)) {
            return false;
        }
        deleteSubscribeRedisKey(key);
        delayQueue.remove(expected);
        return true;
    }

    private void deleteSubscribeRedisKey(String key) {
        String redisKey = String.format("%s_%s_%s", prefix, userSetting.getServerId(), key);
        redisTemplate.delete(redisKey);
    }

    public SipTransactionInfo getTransactionInfo(String key) {
        SubscribeTask task = subscribes.get(key);
        if (task == null) {
            return null;
        }
        return task.getTransactionInfo();
    }

    public boolean updateDelay(String key, long expirationTime) {
        SubscribeTask task = subscribes.get(key);
        if (task == null) {
            return false;
        }
        log.info("[更新订阅任务时间] {}, 编号： {}", task.getName(), key);
        delayQueue.remove(task);
        task.setDelayTime(expirationTime);
        delayQueue.offer(task);
        String redisKey = String.format("%s_%s_%s", prefix, userSetting.getServerId(), task.getKey());
        Duration duration = Duration.ofSeconds((expirationTime - System.currentTimeMillis())/1000);
        redisTemplate.expire(redisKey, duration);
        return true;
    }

    public boolean containsKey(String key) {
        return subscribes.containsKey(key);
    }

    public List<SubscribeTaskInfo> getAllTaskInfo(){
        String scanKey = String.format("%s_%s_*", prefix, userSetting.getServerId());
        List<Object> values = RedisUtil.scan(redisTemplate, scanKey);
        if (values.isEmpty()) {
            return new ArrayList<>();
        }
        List<SubscribeTaskInfo> result = new ArrayList<>();
        for (Object value : values) {
            String redisKey = (String)value;
            SubscribeTaskInfo taskInfo = (SubscribeTaskInfo)redisTemplate.opsForValue().get(redisKey);
            if (taskInfo == null) {
                continue;
            }
            Long expire = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            taskInfo.setExpireTime(expire);
            result.add(taskInfo);
        }
        return result;

    }
}
