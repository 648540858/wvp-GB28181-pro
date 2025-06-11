package com.genersoft.iot.vmp.gat1400.fontend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import cz.data.viid.framework.domain.entity.APEDevice;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.exception.VIIDRuntimeException;

public class SecurityContext {

    public static NodeDevice getNodeDevice() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Objects.nonNull(authentication) && authentication.getPrincipal() instanceof NodeDevice
                ? (NodeDevice) authentication.getPrincipal() : null;
    }

    public static VIIDServer requireVIIDServer() {
        NodeDevice server = getNodeDevice();
        if (Objects.isNull(server))
            throw new VIIDRuntimeException("请先进行注册和保活!");
        if (!server.isServer())
            throw new VIIDRuntimeException("设备类型不正确!");
        return server.originVIIDServer();
    }

    public static APEDevice requireVIIDDevice() {
        NodeDevice device = getNodeDevice();
        if (Objects.isNull(device))
            throw new VIIDRuntimeException("请先进行注册和保活!");
        if (device.isServer())
            throw new VIIDRuntimeException("设备类型不正确!");
        return device.originVIIDDevice();
    }

    public static String getRequestDeviceId() {
        NodeDevice server = getNodeDevice();
        if (Objects.nonNull(server))
            return server.getDeviceId();
        throw new VIIDRuntimeException("security上下文不存在用户凭证");
    }

    public static String getRequestURI() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        return request.getRequestURI();
    }

    public static String getRequestIdentify() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String userIdentify = request.getHeader("User-Identify");
        if (userIdentify == null)
            userIdentify = request.getHeader("user-identify");
        return userIdentify;
    }
}
