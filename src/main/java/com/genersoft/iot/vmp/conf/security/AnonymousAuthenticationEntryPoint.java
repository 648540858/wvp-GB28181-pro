package com.genersoft.iot.vmp.conf.security;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 处理匿名用户访问逻辑
 * @author lin
 */
@Component
public class    AnonymousAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
        String jwt = request.getHeader(JwtUtils.getHeader());
        JwtUser jwtUser = JwtUtils.verifyToken(jwt);
        String username = jwtUser.getUserName();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, jwtUser.getPassword() );
        SecurityContextHolder.getContext().setAuthentication(token);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", ErrorCode.ERROR401.getCode());
        jsonObject.put("msg", ErrorCode.ERROR401.getMsg());
        String logUri = "api/user/login";
        if (request.getRequestURI().contains(logUri)){
            jsonObject.put("msg", e.getMessage());
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().print(jsonObject.toJSONString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
