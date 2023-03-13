package com.genersoft.iot.vmp.conf.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author lin
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final static Logger logger = LoggerFactory.getLogger(LoginSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
//        String username = request.getParameter("username");
//        httpServletResponse.setContentType("application/json;charset=UTF-8");
//        // 生成JWT，并放置到请求头中
//        String jwt = JwtUtils.createToken(authentication.getName(), );
//        httpServletResponse.setHeader(JwtUtils.getHeader(), jwt);
//        ServletOutputStream outputStream = httpServletResponse.getOutputStream();
//        outputStream.write(JSON.toJSONString(ErrorCode.SUCCESS).getBytes(StandardCharsets.UTF_8));
//        outputStream.flush();
//        outputStream.close();

//        logger.info("[登录成功] - [{}]", username);
    }
}
