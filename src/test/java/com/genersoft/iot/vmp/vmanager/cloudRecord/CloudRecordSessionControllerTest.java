package com.genersoft.iot.vmp.vmanager.cloudRecord;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.GlobalExceptionHandler;
import com.genersoft.iot.vmp.conf.GlobalResponseAdvice;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.ICloudRecordSessionService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordSessionContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用真实MVC绑定与统一响应出口验证会话接口契约，不引入生产设备或启云依赖。 */
@ExtendWith(MockitoExtension.class)
class CloudRecordSessionControllerTest {
    private static final String ROOT = "/api/cloud/record/sessions";

    @Mock
    private ICloudRecordSessionService service;
    @Mock
    private UserSetting settings;
    @InjectMocks
    private CloudRecordSessionController controller;

    private MockMvc mvc;

    /** 挂载真实响应包装和异常映射，避免只验证直接Java调用而遗漏HTTP契约。 */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalResponseAdvice(), new GlobalExceptionHandler())
                .build();
    }

    /** 清除线程认证，防止测试之间互相借用用户身份。 */
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /** 创建缺省位置从零开始，关闭认证的空principal保持现有匿名模式。 */
    @Test
    void createUsesZeroPositionAndAnonymousOwnerForNullPrincipal() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(null, null, List.of()));
        when(service.create(0, 21, 0L, null)).thenReturn(sessionContent("anonymous-session"));

        mvc.perform(post(ROOT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cloudRecordId\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value("anonymous-session"))
                .andExpect(jsonPath("$.data.positionMs").value(0));
        verify(service).create(0, 21, 0L, null);
    }

    /** 已认证JWT或APIkey统一的用户principal应传给服务而非读取客户端owner参数。 */
    @Test
    void createPropagatesAuthenticatedUserAndConfiguredStreamHost() throws Exception {
        authenticate(17);
        when(settings.getUseSourceIpAsStreamIp()).thenReturn(true);
        when(service.create(17, 21, 2_500L, "playback.example.test"))
                .thenReturn(sessionContent("owned-session"));

        mvc.perform(post(ROOT).with(request -> {
                            request.setServerName("playback.example.test");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cloudRecordId\":21,\"positionMs\":2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(service).create(17, 21, 2_500L, "playback.example.test");
    }

    /** 缺少或非法录像ID的创建不能进入服务层。 */
    @Test
    void invalidRecordIdIsRejectedBeforeCreatingSession() throws Exception {
        for (String body : List.of("{}", "{\"cloudRecordId\":0}", "{\"cloudRecordId\":-1}")) {
            mvc.perform(post(ROOT).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(service);
    }

    /** 暂停必须提供播放器位置，不能误用默认零位置恢复。 */
    @Test
    void pauseWithoutExplicitPositionIsRejectedBeforeMediaControl() throws Exception {
        mvc.perform(post(ROOT + "/some-session/pause")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(ErrorCode.ERROR400.getMsg()));
        verifyNoInteractions(service);
    }

    /** 定位同样要求明确的毫秒位置，不以空输入触发重建。 */
    @Test
    void seekWithoutExplicitPositionIsRejectedBeforeMediaControl() throws Exception {
        mvc.perform(post(ROOT + "/some-session/seek")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(service);
    }

    /** 查询、拖动、暂停、恢复和关闭均透传服务端会话身份及当前持有人。 */
    @Test
    void everyControlUsesCurrentUserAndExactSessionIdentity() throws Exception {
        authenticate(23);
        when(service.get(23, "exact-session")).thenReturn(sessionContent("exact-session"));
        when(service.seek(23, "exact-session", 9_000L)).thenReturn(sessionContent("exact-session"));
        when(service.pause(23, "exact-session", 12_500L)).thenReturn(sessionContent("exact-session"));
        when(service.resume(23, "exact-session")).thenReturn(sessionContent("exact-session"));

        mvc.perform(get(ROOT + "/exact-session"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post(ROOT + "/exact-session/seek")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"positionMs\":9000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post(ROOT + "/exact-session/pause")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"positionMs\":12500}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post(ROOT + "/exact-session/resume"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(delete(ROOT + "/exact-session"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        verify(service).get(23, "exact-session");
        verify(service).seek(23, "exact-session", 9_000L);
        verify(service).pause(23, "exact-session", 12_500L);
        verify(service).resume(23, "exact-session");
        verify(service).close(23, "exact-session");
        verifyNoMoreInteractions(service);
    }

    /** 保留既有协议错误分类和安全消息，不让统一响应包装覆盖拒绝状态。 */
    @Test
    void ownerRejectionKeepsProtocolCodeAtHttpBoundary() throws Exception {
        authenticate(31);
        when(service.pause(31, "other-session", 1_000L))
                .thenThrow(new ControllerException(ErrorCode.ERROR403));

        mvc.perform(post(ROOT + "/other-session/pause")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"positionMs\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value(ErrorCode.ERROR403.getMsg()));
    }

    /** 服务端安全投影经真实JSON出口后不携带底层节点密钥或磁盘路径。 */
    @Test
    void responseContainsPlaybackUrlsWithoutNodeSecretOrDiskPath() throws Exception {
        CloudRecordSessionContent response = sessionContent("safe-session");
        when(service.create(0, 21, 0L, null)).thenReturn(response);

        MvcResult result = mvc.perform(post(ROOT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cloudRecordId\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playback.app").value("mp4_record"))
                .andExpect(jsonPath("$.data.playback.mediaServerId").value("synthetic-zlm"))
                .andExpect(jsonPath("$.data.playback.mediaServer").doesNotExist())
                .andExpect(content().string(not(containsString("synthetic-secret"))))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("filePath"));
        assertFalse(json.contains("/private/source.mp4"));
        assertFalse(json.contains("\"secret\""));
    }

    /** 暂停响应不返回可继续使用的旧播放器地址。 */
    @Test
    void pausedResponseDoesNotExposeOldPlayback() throws Exception {
        CloudRecordSessionContent response = sessionContent("paused-session");
        response.setState("paused");
        response.setPositionMs(8_000L);
        response.setPlayback(null);
        when(service.pause(0, "paused-session", 8_000L)).thenReturn(response);

        mvc.perform(post(ROOT + "/paused-session/pause")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"positionMs\":8000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("paused"))
                .andExpect(jsonPath("$.data.positionMs").value(8000))
                .andExpect(jsonPath("$.data.playback").doesNotExist());
    }

    /** 创建与现有JWT过滤器相同的User principal，避免测试引入新的身份协议。 */
    private void authenticate(int id) {
        User user = new User();
        user.setId(id);
        user.setUsername("synthetic-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    /** 经过已有StreamContent投影构造响应，使底层敏感字段不会进入JSON。 */
    private static CloudRecordSessionContent sessionContent(String sessionId) {
        MediaServer node = new MediaServer();
        node.setId("synthetic-zlm");
        node.setSecret("synthetic-secret");
        node.setRecordPath("/private/source.mp4");
        StreamInfo info = new StreamInfo();
        info.setApp("mp4_record");
        info.setStream("synthetic-stream");
        info.setMediaServer(node);
        CloudRecordSessionContent content = new CloudRecordSessionContent();
        content.setSessionId(sessionId);
        content.setCloudRecordId(21);
        content.setState("playing");
        content.setDurationMs(60_000L);
        content.setPositionMs(0);
        content.setServerId("synthetic-wvp");
        content.setPlayback(new StreamContent(info));
        return content;
    }
}
