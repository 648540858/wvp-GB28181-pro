package com.genersoft.iot.vmp.conf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DynamicTask 任务注册/停止的并发边界测试。
 *
 * 重点验证 PR #2240 修复的问题：已执行完成的 future 如果残留在任务表里，
 * 会导致同一个 key 的新任务无法注册（收流超时看门狗注册失败，进而点播认领永久残留）。
 */
@ExtendWith(MockitoExtension.class)
class DynamicTaskTest {

    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private DynamicTask dynamicTask;

    @Test
    void stop_shouldRemoveCompletedFuture_andStartDelayShouldRegisterAgain() {
        ScheduledFuture<?> completedFuture = mock(ScheduledFuture.class);
        when(completedFuture.isCancelled()).thenReturn(false);
        when(completedFuture.isDone()).thenReturn(true);

        ScheduledFuture<?> newFuture = mock(ScheduledFuture.class);
        // ScheduledFuture<?> 的通配符捕获会让 thenReturn 出现泛型不匹配，这里使用 doReturn 链
        doReturn(completedFuture).doReturn(newFuture)
                .when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        dynamicTask.startDelay("test-key", () -> { }, 1000);
        assertTrue(dynamicTask.contains("test-key"), "任务应已注册");

        // 任务已经执行完成：stop 不需要 cancel，但必须把它从任务表移除
        assertFalse(dynamicTask.stop("test-key"));
        verify(completedFuture, never()).cancel(false);
        assertFalse(dynamicTask.contains("test-key"), "已完成的任务必须从任务表移除");

        // 同一个 key 再次注册必须成功，否则看门狗会永久缺失
        dynamicTask.startDelay("test-key", () -> { }, 1000);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
        assertTrue(dynamicTask.contains("test-key"), "同 key 任务应能重新注册");
    }

    @Test
    void stop_shouldCancelRunningFuture_andRemoveIt() {
        ScheduledFuture<?> runningFuture = mock(ScheduledFuture.class);
        when(runningFuture.isCancelled()).thenReturn(false);
        when(runningFuture.isDone()).thenReturn(false);
        when(runningFuture.cancel(false)).thenReturn(true);
        doReturn(runningFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        dynamicTask.startDelay("running-key", () -> { }, 1000);

        assertTrue(dynamicTask.stop("running-key"));
        verify(runningFuture).cancel(false);
        assertFalse(dynamicTask.contains("running-key"));
    }

    @Test
    void scheduledCleanup_shouldRemoveCompletedFuture_andHandleConcurrentRemoval() {
        ScheduledFuture<?> completedFuture = mock(ScheduledFuture.class);
        when(completedFuture.isDone()).thenReturn(true);
        doReturn(completedFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        dynamicTask.startDelay("cleanup-key", () -> { }, 1000);

        // execute() 内部遍历时允许其他线程并发删除 key，不能抛 NPE
        dynamicTask.execute();

        assertFalse(dynamicTask.contains("cleanup-key"));
    }
}
