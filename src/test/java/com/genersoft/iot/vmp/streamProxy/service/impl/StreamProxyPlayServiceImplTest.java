package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.conf.DynamicTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamProxyPlayServiceImplTest {

    @Mock
    private StreamProxyMapper streamProxyMapper;

    @Mock
    private IMediaServerService mediaServerService;

    @Mock
    private HookSubscribe subscribe;

    @Mock
    private DynamicTask dynamicTask;

    @Mock
    private UserSetting userSetting;

    @Mock
    private IRedisRpcPlayService redisRpcPlayService;

    @Mock
    private ErrorCallback<StreamInfo> callback;

    @InjectMocks
    private StreamProxyPlayServiceImpl service;

    @Test
    void startProxy_shouldSyncPullingWhenStreamAlreadyExists() {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setEnable(true);
        streamProxy.setServerId("wvp-1");
        streamProxy.setMediaServerId("zlmediakit-local");
        streamProxy.setApp("live");
        streamProxy.setStream("401");
        streamProxy.setPulling(false);
        StreamInfo streamInfo = new StreamInfo();

        when(userSetting.getServerId()).thenReturn("wvp-1");
        when(mediaServerService.getStreamInfoByAppAndStreamWithCheck(
                eq("live"), eq("401"), eq("zlmediakit-local"), eq(null), eq(false)))
                .thenReturn(streamInfo);

        service.startProxy(streamProxy, callback);

        assertTrue(streamProxy.getPulling());
        verify(streamProxyMapper).updateStream(streamProxy);
        verify(callback).run(eq(0), any(), eq(streamInfo));
        verify(mediaServerService, never()).startProxy(any(), any());
    }

    @Test
    void stopProxy_shouldClearWvpStateWhenZlmStreamDoesNotExist() {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setId(8);
        streamProxy.setApp("live");
        streamProxy.setStream("missing");
        when(mediaServerService.getMediaServerByAppAndStream("live", "missing")).thenReturn(null);

        service.stopProxy(streamProxy);

        verify(streamProxyMapper).removeStream(8);
        verify(mediaServerService, never()).closeStreams(any(), any(), any());
    }
}
