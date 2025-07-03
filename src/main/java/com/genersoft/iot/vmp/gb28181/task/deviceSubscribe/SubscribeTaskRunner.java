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
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private UserSetting userSetting;

    private final String prefix = "VMP_DEVICE_SUBSCRIBE";

    // 订阅过期检查
    @Scheduled(fixedDelay = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void expirationCheck(){
        while (!delayQueue.isEmpty()) {
            SubscribeTask take = null;
            try {
                take = delayQueue.take();
                try {
                    removeSubscribe(take.getKey());
                    take.expired();
                }catch (Exception e) {
                    log.error("[设备订阅到期] {} 到期处理时出现异常， 设备编号: {} ", take.getName(), take.getDeviceId());
                }
            } catch (InterruptedException e) {
                log.error("[设备订阅任务] ", e);
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
        String redisKey = String.format("%s_%s_%s", prefix, userSetting.getServerId(), task.getKey());
        redisTemplate.delete(redisKey);
        subscribes.remove(key);
        if (delayQueue.contains(task)) {
            boolean remove = delayQueue.remove(task);
            if (!remove) {
                log.info("[移除订阅任务] 从延时队列内移除失败： {}", key);
            }
        }
        return true;
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
        task.setDelayTime(expirationTime);
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
            Long expire = redisTemplate.getExpire(redisKey);
            taskInfo.setExpireTime(expire);
            result.add(taskInfo);
        }
        return result;

    }
}
