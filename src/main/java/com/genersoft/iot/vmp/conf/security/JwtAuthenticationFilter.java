package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.ArrayList;

/**
 * jwt token 过滤器
 */

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final static String WSHeader = "sec-websocket-protocol";


    @Autowired
    private RuntimeSecurityConfigService runtimeSecurityConfigService;


    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(servletRequest);
        RuntimeSecurityConfig runtimeConfig = runtimeSecurityConfigService.getConfig();
        if (Boolean.TRUE.equals(runtimeConfig.getContentTypeOptionsEnabled())) {
            response.setHeader("X-Content-Type-Options", "nosniff");
        }

        String requestURI = request.getRequestURI();
        if (isDocumentRequest(requestURI) && !Boolean.TRUE.equals(runtimeConfig.getDocEnabled())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (requestURI.equalsIgnoreCase("/api/user/login")) {
            chain.doFilter(request, response);
            return;
        }

        boolean anonymousAllowed = !Boolean.TRUE.equals(runtimeConfig.getInterfaceAuthentication())
                || runtimeSecurityConfigService.isAuthenticationExcluded(requestURI);
        String jwt = resolveToken(request, response);
        if (StringUtils.isBlank(jwt)) {
            if (anonymousAllowed) {
                authenticateAnonymously();
            }
            chain.doFilter(request, response);
            return;
        }

        JwtUser jwtUser = JwtUtils.verifyToken(jwt);
        switch (jwtUser.getStatus()){
            case EXPIRED:
                if (anonymousAllowed) {
                    authenticateAnonymously();
                    chain.doFilter(request, response);
                    return;
                }
                response.setStatus(401);
                chain.doFilter(request, response);
                return;
            case EXCEPTION:
                if (anonymousAllowed) {
                    authenticateAnonymously();
                    chain.doFilter(request, response);
                    return;
                }
                response.setStatus(400);
                chain.doFilter(request, response);
                return;
            case EXPIRING_SOON:
            default:
        }

        User user = new User();
        user.setId(jwtUser.getUserId());
        user.setUsername(jwtUser.getUserName());
        user.setPassword(jwtUser.getPassword());
        Role role = new Role();
        role.setId(jwtUser.getRoleId());
        user.setRole(role);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, jwtUser.getPassword(), new ArrayList<>() );
        SecurityContextHolder.getContext().setAuthentication(token);
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request, HttpServletResponse response) {
        String jwt = request.getHeader(JwtUtils.getHeader());
        if (StringUtils.isNotBlank(jwt)) {
            return jwt;
        }
        String websocketToken = request.getHeader(WSHeader);
        if (StringUtils.isNotBlank(websocketToken)) {
            response.setHeader(WSHeader, websocketToken);
            return websocketToken;
        }
        jwt = request.getParameter(JwtUtils.getHeader());
        return StringUtils.isNotBlank(jwt) ? jwt : request.getHeader(JwtUtils.getApiKeyHeader());
    }

    private boolean isDocumentRequest(String requestUri) {
        return requestUri.startsWith("/doc.html")
                || requestUri.startsWith("/swagger-ui")
                || requestUri.startsWith("/swagger-resources")
                || requestUri.startsWith("/v3/api-docs");
    }

    private void authenticateAnonymously() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                null, null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
