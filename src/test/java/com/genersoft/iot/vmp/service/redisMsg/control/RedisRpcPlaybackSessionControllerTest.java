package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证接收RPC的一侧不丢失独立会话标识，且旧RPC继续走平台共享路径。 */
class RedisRpcPlaybackSessionControllerTest {
    private RedisRpcChannelPlayController controller;
    private IGbChannelPlayService playback;
    private CommonGBChannel channel;

    /** 注入最窄通道查询和分派依赖，不执行异步发送或操作任何流。 */
    @BeforeEach
    void setUp() {
        controller = new RedisRpcChannelPlayController();
        playback = mock(IGbChannelPlayService.class);
        IGbChannelService channels = mock(IGbChannelService.class);
        channel = new CommonGBChannel();
        when(channels.getOne(7)).thenReturn(channel);
        ReflectionTestUtils.setField(controller, "channelService", channels);
        ReflectionTestUtils.setField(controller, "channelPlayService", playback);
    }

    /** 携带标识时直接走独立回放重载，不能被平台startInvite路径吞掉。 */
    @Test
    void receiverForwardsSessionInsteadOfUsingTheLegacyPlatformEntry() {
        RedisRpcRequest request = request("123E4567-E89B-12D3-A456-426614174000");

        assertNull(controller.playbackChannel(request));

        verify(playback).playback(same(channel), eq(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp("2026-10-23 20:40:00")),
                eq(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp("2026-10-23 20:40:20")),
                eq("123e4567e89b12d3a456426614174000"), any());
        verify(playback, never()).startInvite(any(), any(), any(), any());
    }

    /** 没有会话字段的旧请求仍经startInvite调用，不改变平台共享语义。 */
    @Test
    void receiverPreservesOldRequestsWithoutSessionId() {
        assertNull(controller.playbackChannel(request(null)));

        verify(playback).startInvite(same(channel), any(), isNull(), any());
        verify(playback, never()).playback(any(), any(), any(), anyString(), any());
    }

    /** 组装兼容现网的RPC参数，可选字段不存在时保持旧报文形状。 */
    private RedisRpcRequest request(String sessionId) {
        JSONObject params = new JSONObject();
        params.put("channelId", 7);
        params.put("startTime", "2026-10-23 20:40:00");
        params.put("endTime", "2026-10-23 20:40:20");
        if (sessionId != null) params.put("sessionId", sessionId);
        RedisRpcRequest request = new RedisRpcRequest();
        request.setParam(params.toJSONString());
        return request;
    }
}
