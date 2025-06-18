package com.genersoft.iot.vmp.gat1400.backend.security;

import com.genersoft.iot.vmp.gat1400.backend.task.action.KeepaliveAction;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.NodeDevice;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Component
public class VIIDAuthenticationFilter extends VIIDRequestFilter {
    @Autowired
    KeepaliveAction keepaliveAction;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdentify = request.getHeader(getHeaderName(request));
        if (Objects.isNull(authentication) && StringUtils.isNotBlank(userIdentify)) {
            NodeDevice loginUser = keepaliveAction.get(userIdentify);
            if (Objects.nonNull(loginUser) && Boolean.TRUE.equals(loginUser.getOnline())) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                keepaliveAction.keepalive(loginUser.getDeviceId());
            }
        }

        filterChain.doFilter(request, response);
    }

}
