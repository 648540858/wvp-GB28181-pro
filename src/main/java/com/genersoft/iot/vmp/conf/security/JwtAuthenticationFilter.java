package com.genersoft.iot.vmp.conf.security;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * jwt token 过滤器
 */

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final static String WSHeader = "sec-websocket-protocol";


    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IUserService userService;


    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(servletRequest);
        // 忽略登录请求的token验证
        String requestURI = request.getRequestURI();
        if ((requestURI.startsWith("/doc.html") || requestURI.startsWith("/swagger-ui")  ) && !userSetting.getDocEnable()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (requestURI.equalsIgnoreCase("/api/user/login")) {
            chain.doFilter(request, response);
            return;
        }

        if (!userSetting.getInterfaceAuthentication()) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, null, new ArrayList<>() );
            SecurityContextHolder.getContext().setAuthentication(token);
            chain.doFilter(request, response);
            return;
        }

        String jwt = normalizeToken(request.getHeader(JwtUtils.getHeader()));
        // 这里如果没有jwt，继续往后走，因为后面还有鉴权管理器等去判断是否拥有身份凭证，所以是可以放行的
        // 没有jwt相当于匿名访问，若有一些接口是需要权限的，则不能访问这些接口

        // websocket 鉴权信息默认存储在这里
        String secWebsocketProtocolHeader = request.getHeader(WSHeader);
        if (jwt == null) {

            if (secWebsocketProtocolHeader != null) {
                jwt = normalizeToken(secWebsocketProtocolHeader);
                response.setHeader(WSHeader, secWebsocketProtocolHeader);
            }else {
                jwt = normalizeToken(request.getParameter(JwtUtils.getHeader()));
            }
            if (jwt == null) {
                jwt = normalizeToken(request.getHeader(JwtUtils.getApiKeyHeader()));
                if (jwt == null) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        JwtUser jwtUser = JwtUtils.verifyToken(jwt);
        // 注意：token 失效时必须直接结束请求，不能继续 chain.doFilter。
        // 否则浏览器若还持有有效会话，controller 会照常执行并返回成功数据，
        // 最终形成 "HTTP 400/401 + 业务成功数据" 的矛盾响应。
        switch (jwtUser.getStatus()){
            case EXPIRED:
                writeUnauthorized(response, "登录已过期，请重新登录");
                return;
            case EXCEPTION:
                writeUnauthorized(response, "登录凭证无效，请重新登录");
                return;
            case EXPIRING_SOON:
                // 即将过期，继续正常处理
//                return;
            default:
        }
        // 构建UsernamePasswordAuthenticationToken,这里密码为null，是因为提供了正确的JWT,实现自动登录
        User user = new User();
        user.setId(jwtUser.getUserId());
        user.setUsername(jwtUser.getUserName());
        user.setPassword(jwtUser.getPassword());
        Role role = new Role();
        role.setId(jwtUser.getRoleId());
        user.setRole(role);

        // 加载真实用户（含 defaultPassword），用于默认密码访问限制
        User dbUser = userService.getUserById(jwtUser.getUserId());
        user.setDefaultPassword(dbUser != null && dbUser.isDefaultPassword());

        // 默认密码用户：仅允许修改密码与登出接口
        if (user.isDefaultPassword()) {
            if (!requestURI.equalsIgnoreCase("/api/user/changePassword")
                    && !requestURI.equalsIgnoreCase("/api/user/logout")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, jwtUser.getPassword(), new ArrayList<>() );
        SecurityContextHolder.getContext().setAuthentication(token);
        chain.doFilter(request, response);
    }

    /**
     * 前端 cookie 过期后可能把 undefined/null 作为 token 发过来，这里统一按未携带 token 处理
     */
    private String normalizeToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String value = token.trim();
        if ("undefined".equalsIgnoreCase(value) || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    /**
     * 直接返回 401，前端据此清理本地 token 并跳转登录页
     */
    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", ErrorCode.ERROR401.getCode());
        jsonObject.put("msg", msg);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().print(jsonObject.toJSONString());
    }
}
