package com.genersoft.iot.vmp.gat1400.fontend.security;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.BaseResponse;
import com.genersoft.iot.vmp.gat1400.utils.ResponseUtil;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class DataAuthExceptionEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String responseMsg = "";
        if (authException instanceof InvalidBearerTokenException) {
            InvalidBearerTokenException invalidBearerTokenException = (InvalidBearerTokenException) authException;
            String message = invalidBearerTokenException.getMessage();
            if (message.startsWith("An error occurred while attempting to decode the Jwt: Jwt expired at")) {
                responseMsg = "登录过期，请重新登录";
            }
        } else if (authException instanceof CredentialsExpiredException) {
            CredentialsExpiredException expiredException = (CredentialsExpiredException) authException;
            responseMsg = expiredException.getMessage();
        } else {
            responseMsg = "没有权限访问,请先登录";
        }
        ResponseUtil.makeResponse(
                response, MediaType.APPLICATION_JSON_VALUE,
                HttpServletResponse.SC_UNAUTHORIZED, BaseResponse.error(HttpServletResponse.SC_UNAUTHORIZED, responseMsg));
    }
}
