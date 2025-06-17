package com.genersoft.iot.vmp.gat1400.backend.security;

import com.genersoft.iot.vmp.gat1400.backend.service.ISystemService;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.NodeDevice;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.RegisterRequest;
import com.genersoft.iot.vmp.gat1400.framework.exception.VIIDAuthException;
import com.genersoft.iot.vmp.gat1400.listener.event.ServerOnlineEvent;
import com.genersoft.iot.vmp.gat1400.utils.JsonCommon;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.DeviceIdObject;

import lombok.extern.slf4j.Slf4j;

/**
 * VIID摘要认证过滤器
 * todo: 针对任意VIID请求携带User-Identify请求头进行摘要认证时生效。
 * 否则走注册接口逻辑获取body deviceId方式注册
 * 场景: 支持新款海康设备掉线重连时任意请求接口进行摘要认证
 */
@Slf4j
@Component
public class DigestAuthenticationFilter extends VIIDRequestFilter {
    private static final String registerUrl = "/VIID/System/Register";
    @Resource
    DigestAuthenticationEntryPoint entryPoint;
    @Resource
    ISystemService systemService;
    @Value("${VIID_AUTH_VALIDATE:true}")
    Boolean validate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        //检查是否存在摘要认证请求头
        if (header == null || !header.startsWith("Digest ")) {
            if (registerUrl.equals(requestURI) && "POST".equalsIgnoreCase(method)) {
                //如果是第一次注册请求则直接返回401未认证
                fail(request, response, new VIIDAuthException("第一次注册请求"));
            } else {
                //其它请求不需要处理继续走过滤器链
                filterChain.doFilter(request, response);
            }
            return;
        }
        //检查请求头是否包含是设备编号
        String userIdentify = request.getHeader(this.getHeaderName(request));
        //如果请求头设备编号为空并且是注册请求则尝试从body获取设备编号
        if (StringUtils.isBlank(userIdentify) && registerUrl.equals(requestURI)) {
            //包装request使其body能够重复读
            request = new RegisterHttpServletRequestWrapper(request);
            byte[] bytes = ((RegisterHttpServletRequestWrapper) request).getBytes();
            //按照国标协议格式解析注册请求body数据获取设备编号
            RegisterRequest registerRequest = JsonCommon.parseObject(bytes, RegisterRequest.class, false);
            userIdentify = Optional.ofNullable(registerRequest)
                    .map(RegisterRequest::getRegisterObject)
                    .map(DeviceIdObject::getDeviceId)
                    .orElse(null);
            log.info("注册请求body获取设备编号: {}", userIdentify);
        }
        //如果还是获取不到设备编号则不处理摘要认证继续走过滤器链
        if (StringUtils.isBlank(userIdentify)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.info("VIID设备[{}]摘要认证请求: {} - {}", userIdentify, method, requestURI);
        //解析并验证摘要认证头部数据
        DigestData digestData = new DigestData(header);
        try {
            digestData.validateAndDecode(entryPoint.getKey(), entryPoint.getRealmName());
        } catch (BadCredentialsException e) {
            fail(request, response, e);
            return;
        }
        //查找注册设备
        NodeDevice device = systemService.getDeviceById(userIdentify);
        if (device == null) {
            fail(request, response, new VIIDAuthException("未登记设备ID"));
            return;
        }
        //校验摘要认证请求
        String serverDigestMd5 = digestData.calculateServerDigest(device.getPassword(), method.toUpperCase());
        if (serverDigestMd5.equals(digestData.getResponse())) {
            log.info("VIID设备[{}]摘要认证成功: {} - {}", userIdentify, method, requestURI);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(device, null, Collections.emptyList());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            device.setOnline(true);
            SpringContextHolder.publishEvent(new ServerOnlineEvent(device));
            filterChain.doFilter(request, response);
        } else {
            if (registerUrl.equals(requestURI) && Boolean.FALSE.equals(validate)) {
                //todo: 兼容部分设备不校验设备密码
                log.warn("设备[{}]摘要认证请求检验错误放行", userIdentify);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(device, null, Collections.emptyList());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                filterChain.doFilter(request, response);
            } else {
                log.warn("VIID设备[{}]摘要认证请求检验错误 服务端:{} - 客户端:{}", userIdentify, serverDigestMd5, digestData.getResponse());
                fail(request, response, new VIIDAuthException("设备摘要认证错误"));
            }
        }
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);
        this.entryPoint.commence(request, response, failed);
    }

    public static class RegisterHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] bytes;

        public RegisterHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            bytes = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener listener) {

                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(this.getInputStream()));
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
