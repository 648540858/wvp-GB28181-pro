package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务
 */
@Component
public class DynamicTask {

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    public String startCron(String key, Runnable task, String corn) {
        stopCron(key);
        ScheduledFuture future = threadPoolTaskScheduler.schedule(task, new CronTrigger(corn));
        futureMap.put(key, future);
        return "startCron";
    }

    public void stopCron(String key) {
        if (futureMap.get(key) != null && !futureMap.get(key).isCancelled()) {
            futureMap.get(key).cancel(true);
        }
    }

}
