package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.local.RedisTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 平台注册/心跳到期清理的归属校验：drainer 只能清理自己从延时队列取出的任务实例。
 */
@ExtendWith(MockitoExtension.class)
class PlatformStatusTaskRunnerTest {

    private static final String SERVER_ID = "server-1";

    private static final String PLATFORM_SERVER_ID = "platform-1";

    private static final String REDIS_KEY = "VMP_PLATFORM_STATUS_server-1_platform-1";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserSetting userSetting;

    @InjectMocks
    private PlatformStatusTaskRunner runner;

    private Map<String, PlatformRegisterTask> registerSubscribes;

    private DelayQueue<PlatformRegisterTask> registerDelayQueue;

    private Map<String, PlatformKeepaliveTask> keepaliveSubscribes;

    private DelayQueue<PlatformKeepaliveTask> keepaliveTaskDelayQueue;

    @BeforeEach
    void setUp() {
        lenient().when(userSetting.getServerId()).thenReturn(SERVER_ID);
        registerSubscribes = new ConcurrentHashMap<>();
        registerDelayQueue = new DelayQueue<>();
        keepaliveSubscribes = new ConcurrentHashMap<>();
        keepaliveTaskDelayQueue = new DelayQueue<>();
        ReflectionTestUtils.setField(runner, "registerSubscribes", registerSubscribes);
        ReflectionTestUtils.setField(runner, "registerDelayQueue", registerDelayQueue);
        ReflectionTestUtils.setField(runner, "keepaliveSubscribes", keepaliveSubscribes);
        ReflectionTestUtils.setField(runner, "keepaliveTaskDelayQueue", keepaliveTaskDelayQueue);
    }

    /**
     * 注册：同 id 已注册新任务时，旧任务到期不得删除新任务。
     */
    @Test
    void expirationCheckForRegister_shouldKeepNewerTaskForSameId() {
        AtomicBoolean oldExpired = new AtomicBoolean(false);
        AtomicBoolean newExpired = new AtomicBoolean(false);
        PlatformRegisterTask oldTask = registerTask(-1, oldExpired);
        PlatformRegisterTask newTask = registerTask(60_000, newExpired);
        registerSubscribes.put(PLATFORM_SERVER_ID, newTask);
        registerDelayQueue.offer(oldTask);
        registerDelayQueue.offer(newTask);

        runner.expirationCheckForRegister();

        assertTrue(runner.containsRegister(PLATFORM_SERVER_ID));
        assertTrue(registerDelayQueue.contains(newTask));
        assertFalse(oldExpired.get());
        assertFalse(newExpired.get());
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 注册：任务已被显式移除时，到期不得触发回调，也不得删除 Redis。
     */
    @Test
    void expirationCheckForRegister_shouldNotExpireWhenMapEntryWasRemoved() {
        AtomicBoolean oldExpired = new AtomicBoolean(false);
        PlatformRegisterTask oldTask = registerTask(-1, oldExpired);
        registerDelayQueue.offer(oldTask);

        runner.expirationCheckForRegister();

        assertFalse(oldExpired.get());
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 注册：普通到期路径正常清理并触发回调。
     */
    @Test
    void expirationCheckForRegister_shouldExpireMatchingTask() {
        AtomicBoolean expired = new AtomicBoolean(false);
        PlatformRegisterTask task = registerTask(-1, expired);
        registerSubscribes.put(PLATFORM_SERVER_ID, task);
        registerDelayQueue.offer(task);

        runner.expirationCheckForRegister();

        assertFalse(runner.containsRegister(PLATFORM_SERVER_ID));
        assertTrue(expired.get());
        assertFalse(registerDelayQueue.contains(task));
        verify(redisTemplate).delete(REDIS_KEY);
    }

    /**
     * 心跳：同 id 已注册新任务时，旧任务到期不得删除新任务。
     */
    @Test
    void expirationCheckForKeepalive_shouldKeepNewerTaskForSameId() {
        AtomicBoolean oldExpired = new AtomicBoolean(false);
        AtomicBoolean newExpired = new AtomicBoolean(false);
        PlatformKeepaliveTask oldTask = keepaliveTask(-1, oldExpired);
        PlatformKeepaliveTask newTask = keepaliveTask(60_000, newExpired);
        keepaliveSubscribes.put(PLATFORM_SERVER_ID, newTask);
        keepaliveTaskDelayQueue.offer(oldTask);
        keepaliveTaskDelayQueue.offer(newTask);

        runner.expirationCheckForKeepalive();

        assertTrue(runner.containsKeepAlive(PLATFORM_SERVER_ID));
        assertTrue(keepaliveTaskDelayQueue.contains(newTask));
        assertFalse(oldExpired.get());
        assertFalse(newExpired.get());
    }

    /**
     * 心跳：任务已被显式移除时，到期不得触发回调。
     */
    @Test
    void expirationCheckForKeepalive_shouldNotExpireWhenMapEntryWasRemoved() {
        AtomicBoolean oldExpired = new AtomicBoolean(false);
        PlatformKeepaliveTask oldTask = keepaliveTask(-1, oldExpired);
        keepaliveTaskDelayQueue.offer(oldTask);

        runner.expirationCheckForKeepalive();

        assertFalse(oldExpired.get());
    }

    /**
     * 心跳：普通到期路径正常清理并触发回调。
     */
    @Test
    void expirationCheckForKeepalive_shouldExpireMatchingTask() {
        AtomicBoolean expired = new AtomicBoolean(false);
        PlatformKeepaliveTask task = keepaliveTask(-1, expired);
        keepaliveSubscribes.put(PLATFORM_SERVER_ID, task);
        keepaliveTaskDelayQueue.offer(task);

        runner.expirationCheckForKeepalive();

        assertFalse(runner.containsKeepAlive(PLATFORM_SERVER_ID));
        assertTrue(expired.get());
        assertFalse(keepaliveTaskDelayQueue.contains(task));
    }

    private PlatformRegisterTask registerTask(long delayMs, AtomicBoolean expired) {
        return new PlatformRegisterTask(PLATFORM_SERVER_ID, delayMs, null, ignored -> expired.set(true));
    }

    private PlatformKeepaliveTask keepaliveTask(long delayMs, AtomicBoolean expired) {
        return new PlatformKeepaliveTask(PLATFORM_SERVER_ID, delayMs, (platformServerId, failCount) -> expired.set(true));
    }
}
