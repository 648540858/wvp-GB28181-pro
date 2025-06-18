package com.genersoft.iot.vmp.gat1400.backend.security;

import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

public abstract class VIIDRequestFilter extends OncePerRequestFilter {
    private static final String USER_TOKEN = "User-Identify";

    protected String getHeaderName(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (USER_TOKEN.equalsIgnoreCase(headerName)) {
                return headerName;
            }
        }
        return USER_TOKEN;
    }
}
