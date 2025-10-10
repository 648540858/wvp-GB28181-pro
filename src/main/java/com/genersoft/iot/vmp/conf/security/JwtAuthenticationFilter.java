package com.genersoft.iot.vmp.conf.security;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.web.custom.conf.SyTokenManager;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * jwt token 过滤器
 */

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final static String WSHeader = "sec-websocket-protocol";


    @Autowired
    private UserSetting userSetting;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

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
        if (requestURI.startsWith("/api/sy")) {

            // 包装原始请求，缓存请求体
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            if (signCheck(wrappedRequest)) {
                // 使用参数签名方式校验
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, null, new ArrayList<>() );
                SecurityContextHolder.getContext().setAuthentication(token);
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }

        if (!userSetting.getInterfaceAuthentication()) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, null, new ArrayList<>() );
            SecurityContextHolder.getContext().setAuthentication(token);
            chain.doFilter(request, response);
            return;
        }

        String jwt = request.getHeader(JwtUtils.getHeader());
        // 这里如果没有jwt，继续往后走，因为后面还有鉴权管理器等去判断是否拥有身份凭证，所以是可以放行的
        // 没有jwt相当于匿名访问，若有一些接口是需要权限的，则不能访问这些接口

        // websocket 鉴权信息默认存储在这里
        String secWebsocketProtocolHeader = request.getHeader(WSHeader);
        if (StringUtils.isBlank(jwt)) {

            if (secWebsocketProtocolHeader != null) {
                jwt = secWebsocketProtocolHeader;
                response.setHeader(WSHeader, secWebsocketProtocolHeader);
            }else {
                jwt = request.getParameter(JwtUtils.getHeader());
            }
            if (StringUtils.isBlank(jwt)) {
                jwt = request.getHeader(JwtUtils.getApiKeyHeader());
                if (StringUtils.isBlank(jwt)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        JwtUser jwtUser = JwtUtils.verifyToken(jwt);
        String username = jwtUser.getUserName();
        // TODO 处理各个状态
        switch (jwtUser.getStatus()){
            case EXPIRED:
                response.setStatus(401);
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
        user.setId(jwtUser.getUserId());
        user.setUsername(jwtUser.getUserName());
        user.setPassword(jwtUser.getPassword());
        Role role = new Role();
        role.setId(jwtUser.getRoleId());
        user.setRole(role);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, jwtUser.getPassword(), new ArrayList<>() );
        SecurityContextHolder.getContext().setAuthentication(token);
        chain.doFilter(request, response);
    }

    private boolean signCheck(ContentCachingRequestWrapper request) {
        try {
            String sign = request.getParameter("sign");
            String appKey = request.getParameter("appKey");
            String accessToken = request.getParameter("accessToken");
            String timestampStr = request.getParameter("timestamp");

            if (sign == null || appKey == null || accessToken == null || timestampStr == null) {
                log.info("[SY-接口验签] 缺少关键参数：sign/appKey/accessToken/timestamp ");
                return false;
            }
            if (SyTokenManager.INSTANCE.appMap.get(appKey) == null) {
                log.info("[SY-接口验签] appKey {} 对应的 secret 不存在", appKey);
                return false;
            }

            Map<String, String[]> parameterMap = request.getParameterMap();
            parameterMap.remove("sign");
            // 参数排序
            Set<String> paramKeys = new TreeSet<>(parameterMap.keySet());

            // 拼接签名信息
            // 参数拼接
            StringBuilder beforeSign = new StringBuilder();
            for (String paramKey : paramKeys) {
                beforeSign.append(paramKey).append(parameterMap.get(paramKey)[0]);
            }
            // 如果是post请求的json消息，拼接body字符串
            if (request.getContentLength() > 0
                    && request.getMethod().equalsIgnoreCase("POST")
                    && request.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                // 读取body内容
                byte[] requestBodyBytes = request.getContentAsByteArray();

                if (requestBodyBytes.length > 0) {
                    String requestBody = new String(requestBodyBytes, request.getCharacterEncoding());
                    beforeSign.append(requestBody);
                }
            }
            beforeSign.append(SyTokenManager.INSTANCE.appMap.get(appKey));
            // 生成签名
            String buildSign = SmUtil.sm3(beforeSign.toString());
            if (!buildSign.equals(sign)) {
                log.info("[SY-接口验签] 失败， 加密前内容： {}", beforeSign);
                return false;
            }
            // 验证请求时间戳
            Long timestamp = Long.getLong(timestampStr);
            Instant timeInstant = Instant.ofEpochMilli(timestamp + SyTokenManager.INSTANCE.expires * 60 * 1000);
            if (timeInstant.isAfter(Instant.now())) {
                log.info("[SY-接口验签] 时间戳已经过期");
                return false;
            }
            // accessToken校验
            if (accessToken.equals(SyTokenManager.INSTANCE.adminToken)) {
                log.info("[SY-接口验签] 时间戳已经过期");
                return true;
            }else {
                // 对token进行解密
                SM4 sm4 = SmUtil.sm4(HexUtil.decodeHex(SyTokenManager.INSTANCE.sm4Key));
                String decryptStr = sm4.decryptStr(accessToken, CharsetUtil.CHARSET_UTF_8);
                if (decryptStr == null) {
                    log.info("[SY-接口验签] accessToken解密失败");
                    return false;
                }
                JSONObject jsonObject = JSON.parseObject(decryptStr);
                Long expirationTime = jsonObject.getLong("expirationTime");
                if (expirationTime < System.currentTimeMillis()) {
                    log.info("[SY-接口验签] accessToken 已经过期");
                    return false;
                }
            }
        }catch (Exception e) {
            log.info("[SY-接口验签] 读取body失败", e);
            return false;
        }
        return true;


    }

}
