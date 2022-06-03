package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 动态定时任务
 * @author lin
 */
@Component
public class DynamicTask {

    private final Logger logger = LoggerFactory.getLogger(DynamicTask.class);

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Map<String, Runnable> runnableMap = new ConcurrentHashMap<>();

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler schedulerPool = new ThreadPoolTaskScheduler();
        schedulerPool.setPoolSize(300);
        schedulerPool.setWaitForTasksToCompleteOnShutdown(true);
        schedulerPool.setAwaitTerminationSeconds(10);
        return schedulerPool;

    }

    /**
     * 循环执行的任务
     * @param key 任务ID
     * @param task 任务
     * @param cycleForCatalog 间隔 毫秒
     * @return
     */
    public void startCron(String key, Runnable task, int cycleForCatalog) {
        ScheduledFuture<?> future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                logger.debug("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                logger.debug("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， cycleForCatalog表示执行的间隔
        future = threadPoolTaskScheduler.scheduleAtFixedRate(task, cycleForCatalog);
        if (future != null){
            futureMap.put(key, future);
            runnableMap.put(key, task);
            logger.debug("任务【{}】启动成功！！！", key);
        }else {
            logger.debug("任务【{}】启动失败！！！", key);
        }
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

        // 获取执行的时刻
        Instant startInstant = Instant.now().plusMillis(TimeUnit.MILLISECONDS.toMillis(delay));

        ScheduledFuture future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                logger.debug("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                logger.debug("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， cycleForCatalog表示执行的间隔
        future = threadPoolTaskScheduler.schedule(task, startInstant);
        if (future != null){
            futureMap.put(key, future);
            runnableMap.put(key, task);
            logger.debug("任务【{}】启动成功！！！", key);
        }else {
            logger.debug("任务【{}】启动失败！！！", key);
        }
    }

    public void stop(String key) {
        if (futureMap.get(key) != null && !futureMap.get(key).isCancelled()) {
//            Runnable runnable = runnableMap.get(key);
//            if (runnable instanceof ISubscribeTask) {
//                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
//                subscribeTask.stop();
//            }
            futureMap.get(key).cancel(false);
        }
    }

    public boolean contains(String key) {
        return futureMap.get(key) != null;
    }

    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

    public Runnable get(String key) {
        return runnableMap.get(key);
    }
}
