package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * jwt token 过滤器
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    @Autowired
    private UserSetting userSetting;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        // 忽略登录请求的token验证
        String requestURI = request.getRequestURI();
        if (requestURI.equalsIgnoreCase("/api/user/login")) {
            chain.doFilter(request, response);
            return;
        }
        if (!userSetting.isInterfaceAuthentication()) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, null, new ArrayList<>() );
            SecurityContextHolder.getContext().setAuthentication(token);
            chain.doFilter(request, response);
            return;
        }
        String jwt = request.getHeader(JwtUtils.getHeader());
        // 这里如果没有jwt，继续往后走，因为后面还有鉴权管理器等去判断是否拥有身份凭证，所以是可以放行的
        // 没有jwt相当于匿名访问，若有一些接口是需要权限的，则不能访问这些接口
        if (StringUtils.isBlank(jwt)) {
            jwt = request.getParameter(JwtUtils.getHeader());
            if (StringUtils.isBlank(jwt)) {
                chain.doFilter(request, response);
                return;
            }
        }

        JwtUser jwtUser = JwtUtils.verifyToken(jwt);
        String username = jwtUser.getUserName();
        // TODO 处理各个状态
        switch (jwtUser.getStatus()){
            case EXPIRED:
                response.setStatus(400);
                chain.doFilter(request, response);
                // 异常
                return;
            case EXCEPTION:
                // 过期
                response.setStatus(400);
                chain.doFilter(request, response);
                return;
            case EXPIRING_SOON:
                // 即将过期
//                return;
            default:
        }

        // 构建UsernamePasswordAuthenticationToken,这里密码为null，是因为提供了正确的JWT,实现自动登录
        User user = new User();
        user.setUsername(jwtUser.getUserName());
        user.setPassword(jwtUser.getPassword());
        Role role = new Role();
        role.setId(jwtUser.getRoleId());
        user.setRole(role);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, jwtUser.getPassword(), new ArrayList<>() );
        SecurityContextHolder.getContext().setAuthentication(token);
        chain.doFilter(request, response);
    }

}
