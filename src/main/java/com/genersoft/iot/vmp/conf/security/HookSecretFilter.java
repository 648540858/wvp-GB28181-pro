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
     * 媒体服务器回调的路径前缀, 与 ZLMHttpHookListener / ABLHttpHookListener 的 @RequestMapping 一致
     */
    private static final String HOOK_PATH_PREFIX = "/index/hook";

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
        if (!path.startsWith(HOOK_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!hookSecret.equals(request.getParameter("secret"))) {
            log.warn("[媒体服务节点] hook 鉴权失败, 未携带正确的 secret: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
