package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

// 验证RTP同名重开时的异步回调归属与资源清理，避免旧任务关闭新媒体。
@ExtendWith(MockitoExtension.class)
class RtpServerServiceImplTest {
    private static final String APP = "rtp";
    private static final String STREAM = "synthetic-playback-stream";

    @Mock
    private IMediaServerService mediaServerService;
    @Mock
    private DynamicTask dynamicTask;
    @Mock
    private UserSetting userSetting;
    @Mock
    private HookSubscribe subscribe;
    @InjectMocks
    private RtpServerServiceImpl service;

    private final List<Runnable> timeouts = new ArrayList<>();
    private final List<HookSubscribe.Event> registrations = new ArrayList<>();
    private MediaServer mediaServer;
    private RTPServerParam param;

    // 保存调度和Hook的原始引用，模拟cancel前已进入执行阶段的旧回调。
    @BeforeEach
    void setUp() {
        mediaServer = new MediaServer();
        mediaServer.setId("synthetic-media-node");
        mediaServer.setRtpEnable(false);
        mediaServer.setRtpProxyPort(10000);
        param = new RTPServerParam(mediaServer, APP, STREAM, 1200000001L, null, false, false, false, 1);
        when(userSetting.getPlayTimeout()).thenReturn(30000);
        doAnswer(invocation -> {
            timeouts.add(invocation.getArgument(1));
            return null;
        }).when(dynamicTask).startDelay(anyString(), any(Runnable.class), anyInt());
        doAnswer(invocation -> {
            registrations.add(invocation.getArgument(1));
            return null;
        }).when(subscribe).addSubscribe(any(), any());
    }

    // 旧超时在关闭重开后不能关闭新媒体、移除新Hook或通知旧调用方。
    @Test
    void closedAttemptTimeoutCannotAffectReopenedStream() {
        ErrorCallback<HookData> oldCallback = mock(ErrorCallback.class);
        ErrorCallback<HookData> newCallback = mock(ErrorCallback.class);
        assertEquals(10000, service.openCommonRTPServer(param, oldCallback));
        service.closeRTPServer(mediaServer, APP, STREAM);
        assertEquals(10000, service.openCommonRTPServer(param, newCallback));
        clearInvocations(mediaServerService, dynamicTask, subscribe);

        timeouts.getFirst().run();

        verifyNoInteractions(mediaServerService, dynamicTask, subscribe, oldCallback, newCallback);
        HookData data = new HookData();
        registrations.get(1).response(data);
        verify(newCallback).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), data);
    }

    // 旧注册回调在关闭重开后不能取消新定时器或删除新Hook。
    @Test
    void closedAttemptRegistrationCannotRemoveNewSubscription() {
        ErrorCallback<HookData> oldCallback = mock(ErrorCallback.class);
        ErrorCallback<HookData> newCallback = mock(ErrorCallback.class);
        service.openCommonRTPServer(param, oldCallback);
        service.closeRTPServer(mediaServer, APP, STREAM);
        service.openCommonRTPServer(param, newCallback);
        clearInvocations(mediaServerService, dynamicTask, subscribe);

        registrations.getFirst().response(new HookData());

        verifyNoInteractions(mediaServerService, dynamicTask, subscribe, oldCallback, newCallback);
        HookData data = new HookData();
        registrations.get(1).response(data);
        verify(newCallback).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), data);
    }

    // 注册成功只通知一次，之后迟到的同次超时和重复注册都没有副作用。
    @Test
    void successfulAttemptIgnoresLateTimeoutAndDuplicateRegistration() {
        ErrorCallback<HookData> callback = mock(ErrorCallback.class);
        service.openCommonRTPServer(param, callback);
        HookData data = new HookData();
        registrations.getFirst().response(data);
        clearInvocations(mediaServerService, dynamicTask, subscribe);

        timeouts.getFirst().run();
        registrations.getFirst().response(data);

        verify(callback).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), data);
        verifyNoMoreInteractions(callback);
        verifyNoInteractions(mediaServerService, dynamicTask, subscribe);
    }

    // 端口资源失败立即撤销任务和Hook，保存的旧引用不能再次通知失败。
    @Test
    void resourceFailureCleansAttemptAndNotifiesExactlyOnce() {
        ErrorCallback<HookData> callback = mock(ErrorCallback.class);
        mediaServer.setRtpProxyPort(0);

        assertEquals(-1, service.openCommonRTPServer(param, callback));

        verify(callback).run(eq(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode()), anyString(), isNull());
        verify(dynamicTask, times(2)).stop(anyString());
        verify(subscribe, times(2)).removeSubscribe(any());
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(service, "attempts")).isEmpty());
        clearInvocations(mediaServerService, dynamicTask, subscribe);
        timeouts.getFirst().run();
        registrations.getFirst().response(new HookData());
        verifyNoMoreInteractions(callback);
        verifyNoInteractions(mediaServerService, dynamicTask, subscribe);
    }

    // 超时回调也允许另一线程获取RTP锁，避免与上层播放会话锁互相等待。
    @Test
    void timeoutInvokesUpperCallbackOutsideAttemptLock() {
        ErrorCallback<HookData> callback = mock(ErrorCallback.class);
        doAnswer(invocation -> {
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> service.closeRTPServer(mediaServer, APP, STREAM));
            return null;
        }).when(callback).run(anyInt(), anyString(), isNull());
        service.openCommonRTPServer(param, callback);

        timeouts.getFirst().run();

        verify(callback).run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(service, "attempts")).isEmpty());
    }

    // 成功回调可由另一线程立即关闭同流，证明没有持RTP锁进入上层逻辑。
    @Test
    void registrationInvokesUpperCallbackOutsideAttemptLock() {
        ErrorCallback<HookData> callback = mock(ErrorCallback.class);
        doAnswer(invocation -> {
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> service.closeRTPServer(mediaServer, APP, STREAM));
            return null;
        }).when(callback).run(anyInt(), anyString(), any());
        service.openCommonRTPServer(param, callback);

        registrations.getFirst().response(new HookData());

        verify(mediaServerService).closeStreams(mediaServer, APP, STREAM);
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(service, "attempts")).isEmpty());
    }
}
