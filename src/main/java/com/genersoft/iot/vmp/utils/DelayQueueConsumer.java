package com.genersoft.iot.vmp.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.function.Consumer;

/**
 * DelayQueue 专用消费线程。
 * <p>
 * DelayQueue.take() 会阻塞到队首元素到期，不能放在 @Scheduled 方法里执行，
 * 否则会占住 Spring 调度线程，导致其他定时任务（包括报警队列）延迟。
 */
@Slf4j
public final class DelayQueueConsumer {

    private DelayQueueConsumer() {
    }

    public static <T extends Delayed> Thread start(String name, DelayQueue<T> queue, Consumer<T> consumer) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    T item = queue.take();
                    try {
                        consumer.accept(item);
                    } catch (Exception e) {
                        log.error("[DelayQueue] {} 处理异常", name, e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
