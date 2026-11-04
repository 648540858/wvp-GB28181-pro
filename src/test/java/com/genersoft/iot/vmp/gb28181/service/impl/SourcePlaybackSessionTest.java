package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlaybackService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sip.message.Response;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证GB适配层完整传递独立会话和控制动作，旧协议不能假装支持隔离。 */
class SourcePlaybackSessionTest {
    /** 通用通道分派必须保留独立标识，内部旧入口仍传空标识。 */
    @Test
    @SuppressWarnings("unchecked")
    void commonChannelDispatcherPreservesSessionAndLegacyNull() {
        GbChannelPlayServiceImpl dispatcher = new GbChannelPlayServiceImpl();
        ISourcePlaybackService source = mock(ISourcePlaybackService.class);
        ReflectionTestUtils.setField(dispatcher, "sourcePlaybackServiceMap",
                Map.of(ChannelDataType.PLAYBACK_SERVICE + ChannelDataType.GB28181, source));
        CommonGBChannel channel = new CommonGBChannel();
        channel.setDataType(ChannelDataType.GB28181);
        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        String session = "123e4567e89b12d3a456426614174000";

        dispatcher.playback(channel, 1L, 2L, session, callback);
        dispatcher.playback(channel, 1L, 2L, callback);

        verify(source).playback(channel, 1L, 2L, session, callback);
        verify(source).playback(channel, 1L, 2L, null, callback);
    }

    /** 新启动透传会话，恢复和拖动只能调用对应动作并保留原stream。 */
    @Test
    @SuppressWarnings("unchecked")
    void gbSessionAndControlsReachTheMatchingPlaybackOperations() throws Exception {
        IPlayService playService = mock(IPlayService.class);
        SourcePlaybackServiceForGbImpl source = new SourcePlaybackServiceForGbImpl();
        ReflectionTestUtils.setField(source, "playService", playService);
        CommonGBChannel channel = new CommonGBChannel();
        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        String session = "123e4567e89b12d3a456426614174000";

        source.playback(channel, 1L, 2L, session, callback);
        source.playbackResume(channel, "stream-a");
        source.playbackSeek(channel, "stream-b", 15L);

        verify(playService).playBack(channel, 1L, 2L, session, callback);
        verify(playService).playbackResume("stream-a");
        verify(playService).playbackSeek("stream-b", 15L);
        verifyNoMoreInteractions(playService);
    }

    /** 未实现独立会话的协议明确拒绝非空标识，空标识仍按旧方法执行。 */
    @Test
    @SuppressWarnings("unchecked")
    void unsupportedSourceRejectsIsolationWithoutInvokingLegacyPlayback() {
        ISourcePlaybackService source = mock(ISourcePlaybackService.class, CALLS_REAL_METHODS);
        CommonGBChannel channel = new CommonGBChannel();
        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);

        PlayException error = assertThrows(PlayException.class,
                () -> source.playback(channel, 1L, 2L, "123e4567e89b12d3a456426614174000", callback));
        assertEquals(Response.NOT_IMPLEMENTED, error.getCode());
        verify(source, never()).playback(channel, 1L, 2L, callback);
        source.playback(channel, 1L, 2L, null, callback);
        verify(source).playback(channel, 1L, 2L, callback);
        verifyNoInteractions(callback);
    }
}
