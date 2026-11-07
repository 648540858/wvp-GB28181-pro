package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssistRESTfulUtilsTest {

    @Spy
    private AssistRESTfulUtils assistRESTfulUtils;

    // HTTPS 网关访问不能导致内部 Assist 的明文端口收到 TLS 请求。
    @Test
    void addTask_internalHttpsEndpoint_usesHttpForRequestAndCallback() {
        MediaServer mediaServer = mediaServer();
        doReturn(new JSONObject()).when(assistRESTfulUtils)
                .sendPost(eq(mediaServer), anyString(), any(), isNull(), eq(30));

        assistRESTfulUtils.addTask(mediaServer, "rtp", "synthetic-stream", null, null, "synthetic-call",
                List.of("synthetic.mp4"), "https://assist.internal.example:18081");

        ArgumentCaptor<JSONObject> request = ArgumentCaptor.forClass(JSONObject.class);
        verify(assistRESTfulUtils).sendPost(eq(mediaServer),
                eq("http://assist.internal.example:18081/api/record/file/download/task/add"),
                request.capture(), isNull(), eq(30));
        assertEquals("http://assist.internal.example:18081", request.getValue().getString("remoteHost"));
        assertEquals(List.of("synthetic.mp4"), request.getValue().get("filePathList"));
        assertEquals("synthetic-call", request.getValue().getString("callId"));
    }

    // 显式传入的外部 HTTPS 地址必须继续使用 HTTPS。
    @Test
    void addTask_externalHttpsEndpoint_preservesHttps() {
        MediaServer mediaServer = mediaServer();
        doReturn(new JSONObject()).when(assistRESTfulUtils)
                .sendPost(eq(mediaServer), anyString(), any(), isNull(), eq(30));

        assistRESTfulUtils.addTask(mediaServer, "rtp", "synthetic-stream", null, null, null,
                List.of("synthetic.mp4"), "https://public.example:18081");

        ArgumentCaptor<JSONObject> request = ArgumentCaptor.forClass(JSONObject.class);
        verify(assistRESTfulUtils).sendPost(eq(mediaServer),
                eq("https://public.example:18081/api/record/file/download/task/add"),
                request.capture(), isNull(), eq(30));
        assertEquals("https://public.example:18081", request.getValue().getString("remoteHost"));
    }

    // 任务状态查询访问内部端口时保留过滤条件，并使用内部 HTTP 协议。
    @Test
    void queryTaskList_externalHttpsRequest_usesInternalHttp() {
        MediaServer mediaServer = mediaServer();
        doReturn(new JSONObject()).when(assistRESTfulUtils)
                .sendGet(eq(mediaServer), anyString(), anyMap(), isNull());

        assistRESTfulUtils.queryTaskList(mediaServer, "rtp", "synthetic-stream", "synthetic-call",
                "synthetic-task", false, "https");

        ArgumentCaptor<java.util.Map<String, Object>> request = ArgumentCaptor.forClass(java.util.Map.class);
        verify(assistRESTfulUtils).sendGet(eq(mediaServer),
                eq("http://assist.internal.example:18081/api/record/file/download/task/list"),
                request.capture(), isNull());
        assertEquals("synthetic-task", request.getValue().get("taskId"));
        assertEquals(false, request.getValue().get("isEnd"));
    }

    // 构造仅包含内部 Assist 地址的媒体节点，测试不连接真实服务。
    private MediaServer mediaServer() {
        MediaServer mediaServer = new MediaServer();
        mediaServer.setIp("assist.internal.example");
        mediaServer.setRecordAssistPort(18081);
        return mediaServer;
    }
}
