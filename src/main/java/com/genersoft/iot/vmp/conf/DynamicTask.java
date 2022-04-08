package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Set;
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
    private Map<String, Runnable> runnableMap = new ConcurrentHashMap<>();

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    /**
     * 循环执行的任务
     * @param key 任务ID
     * @param task 任务
     * @param cycleForCatalog 间隔
     * @return
     */
    public void startCron(String key, Runnable task, int cycleForCatalog) {
        stop(key);
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， cycleForCatalog表示执行的间隔
        ScheduledFuture future = threadPoolTaskScheduler.scheduleWithFixedDelay(task, cycleForCatalog * 1000L);
        futureMap.put(key, future);
        runnableMap.put(key, task);
    }

    /**
     * 延时任务
     * @param key 任务ID
     * @param task 任务
     * @param delay 延时 /毫秒
     * @return
     */
    public void startDelay(String key, Runnable task, int delay) {
        stop(key);
        Date starTime = new Date(System.currentTimeMillis() + delay);
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， cycleForCatalog表示执行的间隔
        ScheduledFuture future = threadPoolTaskScheduler.schedule(task, starTime);
        futureMap.put(key, future);
    }

    public void stop(String key) {
        if (futureMap.get(key) != null && !futureMap.get(key).isCancelled()) {
            futureMap.get(key).cancel(true);
            Runnable runnable = runnableMap.get(key);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop();
            }
        }
    }

    public boolean contains(String key) {
        return futureMap.get(key) != null;
    }

    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

}
