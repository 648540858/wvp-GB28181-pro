package com.genersoft.iot.vmp.service.redisMsg.service;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证跨节点启动完整透传播放器标识，同时兼容没有会话字段的旧RPC请求。 */
class RedisRpcPlaybackSessionTest {
    private RedisRpcPlayServiceImpl service;
    private RedisRpcConfig config;

    /** 模拟RPC成功响应，不建立Redis连接。 */
    @BeforeEach
    void setUp() {
        service = new RedisRpcPlayServiceImpl();
        config = mock(RedisRpcConfig.class);
        UserSetting settings = mock(UserSetting.class);
        when(settings.getServerId()).thenReturn("source-node");
        when(settings.getPlayTimeout()).thenReturn(30000);
        ReflectionTestUtils.setField(service, "redisRpcConfig", config);
        ReflectionTestUtils.setField(service, "userSetting", settings);
        RedisRpcResponse response = new RedisRpcResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody("{\"stream\":\"target-session-stream\"}");
        when(config.request(any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(response);
    }

    /** 请求所属节点时必须发送规范化会话标识，返回媒体仍交付给原回调。 */
    @Test
    @SuppressWarnings("unchecked")
    void isolatedRequestCarriesCanonicalSessionIdToTheOwningNode() {
        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        service.playback("target-node", 7, "2026-10-23 20:40:00", "2026-10-23 20:40:20",
                "123E4567-E89B-12D3-A456-426614174000", callback);

        ArgumentCaptor<RedisRpcRequest> request = ArgumentCaptor.forClass(RedisRpcRequest.class);
        verify(config).request(request.capture(), eq(30000L), eq(TimeUnit.MILLISECONDS));
        JSONObject params = JSONObject.parseObject(request.getValue().getParam().toString());
        assertEquals("123e4567e89b12d3a456426614174000", params.getString("sessionId"));
        assertEquals("target-node", request.getValue().getToId());
        assertEquals("channel/playback", request.getValue().getUri());
        ArgumentCaptor<StreamInfo> response = ArgumentCaptor.forClass(StreamInfo.class);
        verify(callback).run(eq(ErrorCode.SUCCESS.getCode()), anyString(), response.capture());
        assertEquals("target-session-stream", response.getValue().getStream());
    }

    /** 内部旧入口仍发送原三字段参数，不隐式创建独立会话或添加null字段。 */
    @Test
    @SuppressWarnings("unchecked")
    void legacyRpcDoesNotAddSessionField() {
        service.playback("target-node", 7, "2026-10-23 20:40:00", "2026-10-23 20:40:20", mock(ErrorCallback.class));

        ArgumentCaptor<RedisRpcRequest> request = ArgumentCaptor.forClass(RedisRpcRequest.class);
        verify(config).request(request.capture(), eq(30000L), eq(TimeUnit.MILLISECONDS));
        JSONObject params = JSONObject.parseObject(request.getValue().getParam().toString());
        assertFalse(params.containsKey("sessionId"));
        assertEquals(3, params.size());
    }
}
