package com.genersoft.iot.vmp.gat1400.utils;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DurationUtil {
    private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(3);

    /**
     * 定时执行一个命令函数
     * @param duration 延时时间
     * @param command 命令函数
     */
    public static void schedule(Duration duration, Runnable command) {
        scheduledThreadPool.schedule(command, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static ScheduledExecutorService getScheduledThreadPool() {
        return scheduledThreadPool;
    }
}
