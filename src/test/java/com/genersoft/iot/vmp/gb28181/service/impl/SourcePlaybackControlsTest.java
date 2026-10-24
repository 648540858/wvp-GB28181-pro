package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

/** 验证国标回放适配层把恢复和拖动映射到对应控制命令。 */
class SourcePlaybackControlsTest {
    /** 恢复和拖动只能调用对应动作，并保留请求指定的回放流。 */
    @Test
    void resumeAndSeekReachMatchingPlaybackOperations() throws Exception {
        IPlayService playService = mock(IPlayService.class);
        SourcePlaybackServiceForGbImpl source = new SourcePlaybackServiceForGbImpl();
        ReflectionTestUtils.setField(source, "playService", playService);
        CommonGBChannel channel = new CommonGBChannel();

        source.playbackResume(channel, "stream-a");
        source.playbackSeek(channel, "stream-b", 15L);

        verify(playService).playbackResume("stream-a");
        verify(playService).playbackSeek("stream-b", 15L);
        verifyNoMoreInteractions(playService);
    }
}
