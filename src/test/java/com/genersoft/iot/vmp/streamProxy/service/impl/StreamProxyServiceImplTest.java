package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamProxyServiceImplTest {

    @Mock
    private StreamProxyMapper streamProxyMapper;

    @Mock
    private IStreamProxyPlayService playService;

    @Mock
    private IGbChannelService gbChannelService;

    @InjectMocks
    private StreamProxyServiceImpl service;

    @Test
    void addShouldCreateProxyBeforeRelatedGbChannel() {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setApp("live");
        streamProxy.setStream("camera-1");
        streamProxy.setGbDeviceId("34020000001320000001");
        doAnswer(invocation -> {
            streamProxy.setId(21);
            return 1;
        }).when(streamProxyMapper).add(streamProxy);

        service.add(streamProxy);

        InOrder inOrder = inOrder(streamProxyMapper, gbChannelService);
        inOrder.verify(streamProxyMapper).add(streamProxy);
        inOrder.verify(gbChannelService).add(streamProxy);
    }

    @Test
    void deleteShouldStopZlmBeforeDeletingRelatedWvpData() {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setId(12);
        streamProxy.setPulling(true);
        streamProxy.setGbId(34);
        when(streamProxyMapper.select(12)).thenReturn(streamProxy);

        service.delete(12);

        InOrder inOrder = inOrder(playService, gbChannelService, streamProxyMapper);
        inOrder.verify(playService).stop(12);
        inOrder.verify(gbChannelService).delete(34);
        inOrder.verify(streamProxyMapper).delete(12);
    }

    @Test
    void deleteShouldStopZlmWhenRuntimeMediaServerStateExists() {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setId(13);
        streamProxy.setPulling(false);
        streamProxy.setMediaServerId("zlm-1");
        when(streamProxyMapper.select(13)).thenReturn(streamProxy);

        service.delete(13);

        InOrder inOrder = inOrder(playService, streamProxyMapper);
        inOrder.verify(playService).stop(13);
        inOrder.verify(streamProxyMapper).delete(13);
    }
}
