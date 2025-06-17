package com.genersoft.iot.vmp.gat1400.backend.security;

import com.genersoft.iot.vmp.gat1400.framework.exception.VIIDAuthException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class VIIDAccessDeniedHandler implements AccessDeniedHandler {

    private final DigestAuthenticationEntryPoint entryPoint;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
        log.error("VIID认证过滤链错误: {}", e.getMessage());
        entryPoint.commence(request, response, new VIIDAuthException(e.getMessage()));
    }
}
