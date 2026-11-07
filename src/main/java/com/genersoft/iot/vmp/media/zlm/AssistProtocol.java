package com.genersoft.iot.vmp.media.zlm;

import java.net.URI;

/** 为配置的内部明文 HTTP Assist 端口恢复正确协议。 */
final class AssistProtocol {

    /** 隐藏无状态协议工具的构造入口。 */
    private AssistProtocol() {
    }

    // 只改写显式配置的内部端点，保留外部 HTTPS、其他端口及带认证信息的 URL。
    static String normalize(String endpoint, String mediaServerIp, int assistPort) {
        if (endpoint == null) {
            return null;
        }
        URI uri = URI.create(endpoint);
        if ("https".equalsIgnoreCase(uri.getScheme())
                && mediaServerIp != null && mediaServerIp.equalsIgnoreCase(uri.getHost())
                && assistPort == uri.getPort() && uri.getUserInfo() == null) {
            return "http" + endpoint.substring(endpoint.indexOf(':'));
        }
        return endpoint;
    }
}
