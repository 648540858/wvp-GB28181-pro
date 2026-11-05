package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.utils.DelayQueueConsumer;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PlatformStatusTaskRunner {

    private final Map<String, PlatformRegisterTask> registerSubscribes = new ConcurrentHashMap<>();

    private final DelayQueue<PlatformRegisterTask> registerDelayQueue = new DelayQueue<>();

    private final Map<String, PlatformKeepaliveTask> keepaliveSubscribes = new ConcurrentHashMap<>();

    private final DelayQueue<PlatformKeepaliveTask> keepaliveTaskDelayQueue = new DelayQueue<>();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserSetting userSetting;

    private final String prefix = "VMP_PLATFORM_STATUS";

    private Thread registerConsumerThread;

    private Thread keepaliveConsumerThread;

    @PostConstruct
    public void start() {
        registerConsumerThread = DelayQueueConsumer.start("platform-register-consumer", registerDelayQueue, take -> {
            try {
                // 不在回调前移除任务，避免 statusLostCheck 在注册进行中误判平台未注册。
                // 任务由 registerExpire 在成功(onLine)或失败(offline)时替换/移除。
                take.expired();
            } catch (Exception e) {
                log.error("[平台注册到期] 到期处理时出现异常， 平台上级编号: {} ", take.getPlatformServerId(), e);
                removeRegisterTask(take.getPlatformServerId());
            }
        });
        keepaliveConsumerThread = DelayQueueConsumer.start("platform-keepalive-consumer", keepaliveTaskDelayQueue, take -> {
            try {
                // 不在回调前移除任务，避免 statusLostCheck 在心跳发送过程中误判平台未注册。
                // 任务由 keepaliveExpire 重新添加或移除。
                take.expired();
            } catch (Exception e) {
                log.error("[平台心跳到期] 到期处理时出现异常， 平台上级编号: {} ", take.getPlatformServerId(), e);
                removeKeepAliveTask(take.getPlatformServerId());
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (registerConsumerThread != null) {
            registerConsumerThread.interrupt();
        }
        if (keepaliveConsumerThread != null) {
            keepaliveConsumerThread.interrupt();
        }
    }

    public void addRegisterTask(PlatformRegisterTask task) {
        Duration duration = Duration.ofSeconds((task.getDelayTime() - System.currentTimeMillis())/1000);
        if (duration.getSeconds() < 0) {
            return;
        }
        registerSubscribes.put(task.getPlatformServerId(), task);
        String key = String.format("%s_%s_%s", prefix, userSetting.getServerId(), task.getPlatformServerId());
        redisTemplate.opsForValue().set(key, task.getInfo(), duration);
        registerDelayQueue.offer(task);
    }

    public boolean removeRegisterTask(String platformServerId) {
        PlatformRegisterTask task = registerSubscribes.get(platformServerId);
        if (task != null) {
            registerSubscribes.remove(platformServerId);
        }
        String redisKey = String.format("%s_%s_%s", prefix, userSetting.getServerId(), platformServerId);
        redisTemplate.delete(redisKey);
        if (registerDelayQueue.contains(task)) {
            boolean remove = registerDelayQueue.remove(task);
            if (!remove) {
                log.info("[移除平台注册任务] 从延时队列内移除失败： {}", platformServerId);
            }
        }
        return true;
    }

    public SipTransactionInfo getRegisterTransactionInfo(String platformServerId) {
        PlatformRegisterTask task = registerSubscribes.get(platformServerId);
        if (task == null) {
            return null;
        }
        return task.getSipTransactionInfo();
    }

    public boolean containsRegister(String platformServerId) {
        return registerSubscribes.containsKey(platformServerId);
    }

    public List<PlatformRegisterTaskInfo> getAllRegisterTaskInfo(){
        return getRegisterTransactionInfoByServerId(userSetting.getServerId());
    }

    public void addKeepAliveTask(PlatformKeepaliveTask task) {
        Duration duration = Duration.ofSeconds((task.getDelayTime() - System.currentTimeMillis())/1000);
        if (duration.getSeconds() < 0) {
            return;
        }
        keepaliveSubscribes.put(task.getPlatformServerId(), task);
        keepaliveTaskDelayQueue.offer(task);
    }

    public boolean removeKeepAliveTask(String platformServerId) {
        PlatformKeepaliveTask task = keepaliveSubscribes.get(platformServerId);
        if (task != null) {
            keepaliveSubscribes.remove(platformServerId);
        }
        if (keepaliveTaskDelayQueue.contains(task)) {
            boolean remove = keepaliveTaskDelayQueue.remove(task);
            if (!remove) {
                log.info("[移除平台心跳任务] 从延时队列内移除失败： {}", platformServerId);
            }
        }
        return true;
    }

    public boolean containsKeepAlive(String platformServerId) {
        return keepaliveSubscribes.containsKey(platformServerId);
    }

    /**
     * 当前节点内存中持有任务的平台编号集合（注册任务和心跳任务的并集）
     */
    public Set<String> getAllPlatformIds() {
        Set<String> platformIds = new HashSet<>(registerSubscribes.keySet());
        platformIds.addAll(keepaliveSubscribes.keySet());
        return platformIds;
    }

    public List<PlatformRegisterTaskInfo> getRegisterTransactionInfoByServerId(String serverId) {
        String scanKey = String.format("%s_%s_*", prefix, serverId);
        List<Object> values = RedisUtil.scan(redisTemplate, scanKey);
        if (values.isEmpty()) {
            return new ArrayList<>();
        }
        List<PlatformRegisterTaskInfo> result = new ArrayList<>();
        for (Object value : values) {
            String redisKey = (String)value;
            PlatformRegisterTaskInfo taskInfo = (PlatformRegisterTaskInfo)redisTemplate.opsForValue().get(redisKey);
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
