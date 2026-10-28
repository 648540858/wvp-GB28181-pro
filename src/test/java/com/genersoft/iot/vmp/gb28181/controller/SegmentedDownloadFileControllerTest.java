package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.GlobalResponseAdvice;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.AnonymousAuthenticationEntryPoint;
import com.genersoft.iot.vmp.conf.security.DefaultUserDetailsServiceImpl;
import com.genersoft.iot.vmp.conf.security.JwtAuthenticationFilter;
import com.genersoft.iot.vmp.conf.security.LogoutHandler;
import com.genersoft.iot.vmp.conf.security.WebSecurityConfig;
import com.genersoft.iot.vmp.gb28181.service.download.SegmentedArtifactFiles;
import com.genersoft.iot.vmp.service.IUserService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 验证无Cookie成品下载的原始字节、HTTP区间和实际SecurityFilterChain授权边界。 */
class SegmentedDownloadFileControllerTest {
    private static final String TOKEN = "a".repeat(64);
    private static final String URL = "/record-download/" + TOKEN + ".mp4";
    @TempDir Path root;
    private SegmentedArtifactFiles files;
    private MockMvc mvc;
    private byte[] bytes;

    /** 装入现有全局Advice，确保成品不会被意外包装为JSON。 */
    @BeforeEach
    void setUp() throws Exception {
        files = mock(SegmentedArtifactFiles.class);
        bytes = new byte[64]; for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) index;
        bytes[4] = 'f'; bytes[5] = 't'; bytes[6] = 'y'; bytes[7] = 'p';
        Path artifact = Files.write(root.resolve("artifact.mp4"), bytes);
        when(files.resolve(TOKEN)).thenReturn(artifact);
        mvc = MockMvcBuilders.standaloneSetup(new SegmentedDownloadFileController(files))
                .setControllerAdvice(new GlobalResponseAdvice()).build();
    }

    /** 完整GET逐字节保留MP4；HEAD仅返回真实Content-Length而不写响应体。 */
    @Test
    void getAndHeadReturnRawFileWithLength() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(header().string("Content-Length", "64"))
                .andExpect(header().string("Content-Type", "video/mp4"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "private, no-store"));
        mvc.perform(head(URL)).andExpect(status().isOk()).andExpect(content().bytes(new byte[0]))
                .andExpect(header().string("Content-Length", "64"));
    }

    /** SDK首32字节探测、后缀、开放尾端和超过文件尾的范围保持标准单区间语义。 */
    @Test
    void supportsSingleRangesAndRangeHead() throws Exception {
        mvc.perform(get(URL).header("Range", "bytes=0-31")).andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-31/64"))
                .andExpect(header().string("Content-Length", "32"))
                .andExpect(content().bytes(java.util.Arrays.copyOfRange(bytes, 0, 32)));
        mvc.perform(get(URL).header("Range", "bytes=-4")).andExpect(status().isPartialContent())
                .andExpect(content().bytes(java.util.Arrays.copyOfRange(bytes, 60, 64)));
        mvc.perform(get(URL).header("Range", "bytes=60-")).andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 60-63/64"));
        mvc.perform(head(URL).header("Range", "bytes=60-999")).andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Length", "4")).andExpect(content().bytes(new byte[0]));
        mvc.perform(get(URL).header("Range", "bytes=0-3").header("If-Range", "\"unknown\""))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes));
    }

    /** 多区间、溢出、越界和畸形输入统一416，且不返回文件或JSON错误体。 */
    @Test
    void rejectsMalformedAndMultipleRanges() throws Exception {
        for (String range : new String[]{"bytes=0-1,4-5", "bytes=64-", "bytes=5-4", "bytes=-0", "bytes=0-9223372036854775808",
                "items=0-2", "bytes= -2", "bytes=--1"}) {
            mvc.perform(get(URL).header("Range", range)).andExpect(status().isRequestedRangeNotSatisfiable())
                    .andExpect(header().string("Content-Range", "bytes */64"))
                    .andExpect(header().string("Content-Length", "0")).andExpect(content().bytes(new byte[0]));
        }
        mvc.perform(get(URL).header("Range", "bytes=0-1", "bytes=2-3"))
                .andExpect(status().isRequestedRangeNotSatisfiable());
    }

    /** 未知与失效令牌都是404，Redis故障失败关闭且不输出内部异常。 */
    @Test
    void missingAndUnavailableTokensNeverReturnBytes() throws Exception {
        mvc.perform(get("/record-download/unknown.mp4")).andExpect(status().isNotFound()).andExpect(content().bytes(new byte[0]));
        when(files.resolve(TOKEN)).thenThrow(new IllegalStateException("secret internal path"));
        mvc.perform(get(URL)).andExpect(status().isServiceUnavailable()).andExpect(content().bytes(new byte[0]));
    }

    /** 挂载真实安全配置，证明只有成品GET/HEAD可匿名，写方法和其他接口仍受认证约束。 */
    @Test
    void realSecurityChainOnlyPermitsAnonymousReadMethods() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext()); context.register(SecurityContext.class); context.refresh();
            SegmentedArtifactFiles secured = context.getBean(SegmentedArtifactFiles.class);
            Path artifact = Files.write(root.resolve("secured.mp4"), bytes); when(secured.resolve(TOKEN)).thenReturn(artifact);
            MockMvc securedMvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
            securedMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
            securedMvc.perform(head(URL)).andExpect(status().isOk()).andExpect(content().bytes(new byte[0]));
            securedMvc.perform(get("/record-download/unknown.mp4")).andExpect(status().isNotFound());
            securedMvc.perform(post(URL)).andExpect(status().isUnauthorized());
            securedMvc.perform(delete(URL)).andExpect(status().isUnauthorized());
            securedMvc.perform(get("/record-download/nested/" + TOKEN + ".mp4")).andExpect(status().isUnauthorized());
            securedMvc.perform(get("/api/device/query/devices")).andExpect(status().isUnauthorized());
        }
    }

    /** 提供实际WebSecurityConfig的最小依赖，保持接口认证开启。 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(WebSecurityConfig.class)
    static class SecurityContext {
        /** 开启认证，防止测试在全站免登录模式下误通过。 */
        @Bean UserSetting userSetting() { UserSetting setting = new UserSetting(); setting.setInterfaceAuthentication(true); return setting; }
        /** 使用真实JWT过滤器验证匿名请求继续进入认证授权链。 */
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter() { return new JwtAuthenticationFilter(); }
        /** 未登录统一响应401，不依赖测试环境中的JWT密钥。 */
        @Bean AnonymousAuthenticationEntryPoint anonymousAuthenticationEntryPoint() {
            AnonymousAuthenticationEntryPoint entry = mock(AnonymousAuthenticationEntryPoint.class);
            doAnswer(call -> { ((jakarta.servlet.http.HttpServletResponse) call.getArgument(1)).setStatus(401); return null; })
                    .when(entry).commence(any(), any(), any());
            return entry;
        }
        /** 用户查询不参与成品读取。 */
        @Bean IUserService userService() { return mock(IUserService.class); }
        /** 登录服务仅用于现有认证管理器初始化。 */
        @Bean DefaultUserDetailsServiceImpl userDetailsService() { return mock(DefaultUserDetailsServiceImpl.class); }
        /** 登出仍由现有安全链独立管理。 */
        @Bean LogoutHandler logoutHandler() { return mock(LogoutHandler.class); }
        /** 控制器仅依赖成品令牌解析。 */
        @Bean SegmentedArtifactFiles artifactFiles() { return mock(SegmentedArtifactFiles.class); }
        /** 注册真实的成品控制器。 */
        @Bean SegmentedDownloadFileController controller(SegmentedArtifactFiles files) { return new SegmentedDownloadFileController(files); }
        /** 与生产相同地装入全局响应Advice。 */
        @Bean GlobalResponseAdvice advice() { return new GlobalResponseAdvice(); }
    }
}
