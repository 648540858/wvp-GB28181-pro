package com.genersoft.iot.vmp.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpPortUtil {

    /**
     * 拼接IP和端口
     * @param ip IP地址字符串
     * @param port 端口号字符串
     * @return 拼接后的字符串
     * @throws IllegalArgumentException 如果IP地址无效或端口无效
     */
    public static String concatenateIpAndPort(String ip, String port) {
        if (port == null || port.isEmpty()) {
            throw new IllegalArgumentException("端口号不能为空");
        }

        // 验证端口是否为有效数字
        try {
            int portNum = Integer.parseInt(port);
            if (portNum < 0 || portNum > 65535) {
                throw new IllegalArgumentException("端口号必须在0-65535范围内");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("端口号必须是有效数字", e);
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ip);

            if (inetAddress instanceof Inet6Address) {
                // IPv6地址需要加上方括号
                return "[" + ip + "]:" + port;
            } else {
                // IPv4地址直接拼接
                return ip + ":" + port;
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无效的IP地址: " + ip, e);
        }
    }
}
