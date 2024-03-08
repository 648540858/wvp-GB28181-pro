package com.genersoft.iot.vmp.conf;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Map<String, Runnable> runnableMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void DynamicTask() {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(300);
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setAwaitTerminationSeconds(10);
        threadPoolTaskScheduler.initialize();
    }

    /**
     * 循环执行的任务
     * @param key 任务ID
     * @param task 任务
     * @param cycleForCatalog 间隔 毫秒
     * @return
     */
    public void startCron(String key, Runnable task, int cycleForCatalog) {
        if(ObjectUtils.isEmpty(key)) {
            return;
        }
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
        if(ObjectUtils.isEmpty(key)) {
            return;
        }
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

    public boolean stop(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return false;
        }
        boolean result = false;
        if (!ObjectUtils.isEmpty(futureMap.get(key)) && !futureMap.get(key).isCancelled() && !futureMap.get(key).isDone()) {
            result = futureMap.get(key).cancel(false);
            futureMap.remove(key);
            runnableMap.remove(key);
        }
        return result;
    }

    public boolean contains(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return false;
        }
        return futureMap.get(key) != null;
    }

    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

    public Runnable get(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return null;
        }
        return runnableMap.get(key);
    }

    /**
     * 每五分钟检查失效的任务，并移除
     */
    @Scheduled(cron="0 0/5 * * * ?")
    public void execute(){
        if (futureMap.size() > 0) {
            for (String key : futureMap.keySet()) {
                ScheduledFuture<?> future = futureMap.get(key);
                if (future.isDone() || future.isCancelled()) {
                    futureMap.remove(key);
                    runnableMap.remove(key);
                }
            }
        }
    }

    public boolean isAlive(String key) {
        return futureMap.get(key) != null && !futureMap.get(key).isDone() && !futureMap.get(key).isCancelled();
    }
}
