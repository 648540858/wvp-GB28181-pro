package com.genersoft.iot.vmp.gat1400.backend.security;

import com.genersoft.iot.vmp.gat1400.framework.exception.VIIDAuthException;
import com.genersoft.iot.vmp.gat1400.utils.Digests;

import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Component
public class DigestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final String key = DigestData.DIGEST_KEY;

    private final String realmName = DigestData.DIGEST_REALM;

    private final int nonceValiditySeconds = 300;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        long expiryTime = System.currentTimeMillis() + (this.getNonceValiditySeconds() * 1000L);
        String signatureValue = DigestAuthUtils.md5Hex(expiryTime + ":" + this.getKey());
        String nonceValue = expiryTime + ":" + signatureValue;
        String nonceValueBase64 = new String(Base64.getEncoder().encode(nonceValue.getBytes()));
        String cnonce = new String(Hex.encodeHex(Digests.generateSalt(8)));
        String authenticateHeader = "Digest realm=\"" + this.getRealmName() + "\", qop=\"auth\", nonce=\"" + nonceValueBase64 + "\", opaque=\"" + signatureValue + "\", cnonce=\"" + cnonce + "\"";
        response.addHeader("WWW-Authenticate", authenticateHeader);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        String message = getTitle(e);
        log.warn(message + ": {} -{}, client: {}", request.getMethod(), request.getRequestURI(), request.getRemoteHost());
        StringBuilder sb = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            sb.append(headerName).append(": ").append(request.getHeader(headerName)).append("; ");
        }
        log.info(message + "包含请求头: {}", sb);
    }

    private String getTitle(AuthenticationException e) {
        if (e instanceof InsufficientAuthenticationException) {
            return "未注册保活请求";
        } else if (e instanceof VIIDAuthException) {
            return e.getMessage();
        } else {
            log.warn("摘要认证未知异常: {}", e.getClass());
            return "未授权请求";
        }
    }
}
