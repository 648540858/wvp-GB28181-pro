package com.genersoft.iot.vmp.conf.trace;

import com.genersoft.iot.vmp.conf.SipConfig;

/**
 * 从 sip.showIp / monitorIps 中挑选一个稳定的局域网 IP。
 * <p>
 * showIp 可能是多个网卡 IP 的逗号分隔字符串，直接展示会变成
 * "172.18.0.1,172.19.0.1,192.168.1.3"，这里统一取一个可用的局域网 IP。
 */
public final class TraceIpUtil {

    private TraceIpUtil() {
    }

    public static String resolveSipShowIp(SipConfig sipConfig) {
        String ip = pickLanIp(sipConfig.getShowIp());
        if (ip != null) {
            return ip;
        }
        if (sipConfig.getMonitorIps() != null && !sipConfig.getMonitorIps().isEmpty()) {
            for (String monitorIp : sipConfig.getMonitorIps()) {
                String picked = pickLanIp(monitorIp);
                if (picked != null) {
                    return picked;
                }
            }
            return sipConfig.getMonitorIps().get(0);
        }
        return "unknown";
    }

    public static String pickLanIp(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] parts = raw.split("[,;\\s]+");
        String fallback = null;
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            String candidate = part;
            int portIndex = candidate.lastIndexOf(':');
            if (portIndex > 0) {
                candidate = candidate.substring(0, portIndex);
            }
            if (fallback == null) {
                fallback = candidate;
            }
            if (candidate.startsWith("192.168.")) {
                return candidate;
            }
            if (candidate.startsWith("127.")
                    || candidate.startsWith("169.254.")
                    || candidate.startsWith("0.0.0.0")
                    || candidate.startsWith("172.17.")
                    || candidate.startsWith("172.18.")
                    || candidate.startsWith("172.19.")
                    || candidate.startsWith("172.20.")
                    || candidate.startsWith("172.21.")
                    || candidate.startsWith("172.22.")
                    || candidate.startsWith("172.23.")
                    || candidate.startsWith("172.24.")
                    || candidate.startsWith("172.25.")
                    || candidate.startsWith("172.26.")
                    || candidate.startsWith("172.27.")
                    || candidate.startsWith("172.28.")
                    || candidate.startsWith("172.29.")
                    || candidate.startsWith("172.30.")
                    || candidate.startsWith("172.31.")) {
                continue;
            }
            return candidate;
        }
        return fallback;
    }
}
