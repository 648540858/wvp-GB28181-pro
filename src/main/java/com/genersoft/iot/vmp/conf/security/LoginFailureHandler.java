package com.genersoft.iot.vmp.conf.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final static Logger logger = LoggerFactory.getLogger(LoginFailureHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {

        String username = request.getParameter("username");
        if (e instanceof AccountExpiredException) {
            // 账号过期
            logger.info("[登录失败] - 用户[{}]账号过期", username);

        } else if (e instanceof BadCredentialsException) {
            // 密码错误
            logger.info("[登录失败] - 用户[{}]密码/SIP服务器ID 错误", username);

        } else if (e instanceof CredentialsExpiredException) {
            // 密码过期
            logger.info("[登录失败] - 用户[{}]密码过期", username);

        } else if (e instanceof DisabledException) {
            // 用户被禁用
            logger.info("[登录失败] - 用户[{}]被禁用", username);

        } else if (e instanceof LockedException) {
            // 用户被锁定
            logger.info("[登录失败] - 用户[{}]被锁定", username);

        } else if (e instanceof InternalAuthenticationServiceException) {
            // 内部错误
            logger.error(String.format("[登录失败] - [%s]内部错误", username), e);

        } else {
            // 其他错误
            logger.error(String.format("[登录失败] - [%s]其他错误", username), e);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("code","0");
        map.put("msg","登录失败");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(map));
    }
}
