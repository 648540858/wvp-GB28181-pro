package com.genersoft.iot.vmp.conf.redis;

import com.genersoft.iot.vmp.conf.UserSetting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisRpcConfigTest {

    private static final String SELF_REQUEST = "{\"request\":{\"fromId\":\"server\"}}";

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldConsumeMessageArrivingAsPreviousConsumerStops() throws Exception {
        RedisRpcConfig redisRpcConfig = new RedisRpcConfig();
        UserSetting userSetting = new UserSetting();
        userSetting.setServerId("server");
        RaceQueue taskQueue = new RaceQueue();
        AtomicInteger submittedTasks = new AtomicInteger();
        CountDownLatch secondConsumerFinished = new CountDownLatch(1);
        TaskExecutor taskExecutor =
                task -> {
                    int taskNumber = submittedTasks.incrementAndGet();
                    executorService.execute(
                            () -> {
                                try {
                                    task.run();
                                } finally {
                                    if (taskNumber == 1) {
                                        taskQueue.firstConsumerFinished.countDown();
                                    } else if (taskNumber == 2) {
                                        secondConsumerFinished.countDown();
                                    }
                                }
                            });
                };
        ReflectionTestUtils.setField(redisRpcConfig, "userSetting", userSetting);
        ReflectionTestUtils.setField(redisRpcConfig, "taskQueue", taskQueue);
        ReflectionTestUtils.setField(redisRpcConfig, "taskExecutor", taskExecutor);

        redisRpcConfig.onMessage(new TestMessage(SELF_REQUEST), null);
        Future<?> secondProducer =
                executorService.submit(
                        () -> redisRpcConfig.onMessage(new TestMessage(SELF_REQUEST), null));

        secondProducer.get(5, TimeUnit.SECONDS);
        assertTrue(
                secondConsumerFinished.await(5, TimeUnit.SECONDS),
                "The message arriving during consumer shutdown was not scheduled");
        assertEquals(2, submittedTasks.get());
        assertTrue(taskQueue.isEmpty());
    }

    private static class RaceQueue extends ConcurrentLinkedQueue<Message> {

        private final AtomicInteger offerCount = new AtomicInteger();
        private final AtomicInteger pollCount = new AtomicInteger();
        private final CountDownLatch secondOfferStarted = new CountDownLatch(1);
        private final CountDownLatch firstConsumerFinished = new CountDownLatch(1);

        @Override
        public boolean offer(Message message) {
            if (offerCount.incrementAndGet() == 2) {
                secondOfferStarted.countDown();
                await(firstConsumerFinished, "The first consumer did not stop");
            }
            return super.offer(message);
        }

        @Override
        public Message poll() {
            if (pollCount.incrementAndGet() == 1) {
                await(secondOfferStarted, "The second producer did not reach the queue");
            }
            return super.poll();
        }

        private static void await(CountDownLatch latch, String message) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError(message);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(message, exception);
            }
        }
    }

    private static class TestMessage implements Message {

        private final byte[] body;

        private TestMessage(String body) {
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] getBody() {
            return body;
        }

        @Override
        public byte[] getChannel() {
            return new byte[0];
        }
    }
}
