package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * [可选] 媒体服务器 hook 回调鉴权。
 *
 * user-settings.hook-secret 为空时不做任何处理(保持原有行为)。
 * 配置后, wvp 推送的 hook URL 会带上 ?secret=xxx, 本过滤器要求 /index/hook/** 的请求
 * 携带同一个 secret, 否则直接拒绝, 避免任何人都能伪造媒体服务器的回调。
 *
 * 执行顺序说明: 本类是 @Component 的 servlet 过滤器, Spring Boot 会把它自动注册到 /*。
 * 这里显式指定 order, 让它排在 Spring Security 过滤器链之前, 因此不依赖
 * WebSecurityConfig 中 /index/hook/** 的 permitAll 是否短路(permitAll 不会短路, 但显式排序更稳)。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class HookSecretFilter extends OncePerRequestFilter {

    /**
     * 媒体服务器回调的路径前缀, 与 ZLMHttpHookListener / ABLHttpHookListener 的 @RequestMapping 一致。
     * 只匹配 "/index/hook" 本身或 "/index/hook/" 前缀, 避免把 "/index/hookxyz" 之类的路径也拦下来。
     */
    private static final String HOOK_PATH_PREFIX = "/index/hook";

    private static final String HOOK_PATH_PREFIX_WITH_SLASH = HOOK_PATH_PREFIX + "/";

    @Autowired
    private UserSetting userSetting;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String hookSecret = userSetting.getHookSecret();
        if (ObjectUtils.isEmpty(hookSecret)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!HOOK_PATH_PREFIX.equals(path) && !path.startsWith(HOOK_PATH_PREFIX_WITH_SLASH)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isSameSecret(hookSecret, request.getParameter("secret"))) {
            log.warn("[媒体服务节点] hook 鉴权失败, 未携带正确的 secret: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 常量时间比较, 避免逐字符比较短路带来的时序差异。
     *
     * MessageDigest#isEqual 的入参是 byte[], 且在长度不同时会提前返回 false,
     * 所以这里先做 null 保护与长度判断, 只在长度相同时进入逐字节比较。
     */
    private static boolean isSameSecret(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
