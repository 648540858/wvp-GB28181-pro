package com.genersoft.iot.vmp.conf.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.session.InvalidSessionStrategy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录超时的处理
 */
public class InvalidSessionHandler implements InvalidSessionStrategy {

    private final static Logger logger = LoggerFactory.getLogger(InvalidSessionHandler.class);

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        String username = request.getParameter("username");
        logger.info("[登录超时] - [{}]", username);
    }
}
