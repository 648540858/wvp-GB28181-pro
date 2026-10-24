package com.genersoft.iot.vmp.gb28181.task.deviceSubscribe;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.local.RedisTemplate;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl.SubscribeTaskForCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 设备订阅到期清理的归属校验：drainer 只能清理自己从延时队列取出的任务实例。
 */
@ExtendWith(MockitoExtension.class)
class SubscribeTaskRunnerTest {

    private static final String SERVER_ID = "server-1";

    private static final String DEVICE_ID = "device-1";

    private static final String KEY = "catalog_device-1";

    private static final String REDIS_KEY = "VMP_DEVICE_SUBSCRIBE_server-1_catalog_device-1";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserSetting userSetting;

    @InjectMocks
    private SubscribeTaskRunner runner;

    private Map<String, SubscribeTask> subscribes;

    private DelayQueue<SubscribeTask> delayQueue;

    @BeforeEach
    void setUp() {
        lenient().when(userSetting.getServerId()).thenReturn(SERVER_ID);
        subscribes = new ConcurrentHashMap<>();
        delayQueue = new DelayQueue<>();
        ReflectionTestUtils.setField(runner, "subscribes", subscribes);
        ReflectionTestUtils.setField(runner, "delayQueue", delayQueue);
    }

    /**
     * 同 key 已注册新任务时，旧任务到期不得删除新任务。
     */
    @Test
    void expirationCheck_shouldKeepNewerTaskForSameKey() {
        AtomicBoolean oldExpired = new AtomicBoolean(false);
        AtomicBoolean newExpired = new AtomicBoolean(false);
        SubscribeTask oldTask = task(-1, oldExpired);
        SubscribeTask newTask = task(60_000, newExpired);
        subscribes.put(KEY, newTask);
        delayQueue.offer(oldTask);
        delayQueue.offer(newTask);

        runner.expirationCheck();

        assertTrue(runner.containsKey(KEY));
        assertTrue(delayQueue.contains(newTask));
        assertFalse(oldExpired.get());
        assertFalse(newExpired.get());
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 任务已被显式移除（map 无该 key）时，到期不得触发回调，也不得删除 Redis。
     */
    @Test
    void expirationCheck_shouldNotExpireWhenMapEntryWasRemoved() {
        AtomicBoolean oldExpired = new AtomicBoolean(false);
        SubscribeTask oldTask = task(-1, oldExpired);
        delayQueue.offer(oldTask);

        runner.expirationCheck();

        assertFalse(oldExpired.get());
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 普通到期路径：map 中的任务与出队任务同一个实例时，正常清理并触发回调。
     */
    @Test
    void expirationCheck_shouldExpireMatchingTask() {
        AtomicBoolean expired = new AtomicBoolean(false);
        SubscribeTask task = task(-1, expired);
        subscribes.put(KEY, task);
        delayQueue.offer(task);

        runner.expirationCheck();

        assertFalse(runner.containsKey(KEY));
        assertTrue(expired.get());
        assertFalse(delayQueue.contains(task));
        verify(redisTemplate).delete(REDIS_KEY);
    }

    /**
     * 出队后、清理前被 updateDelay 续期时，drainer 必须跳过，不能触发到期回调。
     */
    @Test
    void expirationCheck_shouldNotExpireTaskRenewedAfterPoll() {
        AtomicBoolean expired = new AtomicBoolean(false);
        SubscribeTask task = task(-1, expired);
        DelayQueue<SubscribeTask> renewBeforeReturnQueue = new DelayQueue<SubscribeTask>() {
            @Override
            public SubscribeTask poll() {
                SubscribeTask polled = super.poll();
                if (polled == task) {
                    runner.updateDelay(KEY, System.currentTimeMillis() + 60_000);
                }
                return polled;
            }
        };
        subscribes.put(KEY, task);
        renewBeforeReturnQueue.offer(task);
        ReflectionTestUtils.setField(runner, "delayQueue", renewBeforeReturnQueue);

        runner.expirationCheck();

        assertTrue(runner.containsKey(KEY));
        assertTrue(renewBeforeReturnQueue.contains(task));
        assertFalse(expired.get());
        verify(redisTemplate).expire(eq(REDIS_KEY), any(Duration.class));
    }

    private SubscribeTask task(long delayMs, AtomicBoolean expired) {
        SubscribeTask task = new SubscribeTaskForCatalog();
        task.setDeviceId(DEVICE_ID);
        task.setDelayTime(System.currentTimeMillis() + delayMs);
        task.setCallback((deviceId, transactionInfo) -> expired.set(true));
        return task;
    }
}
