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

    // 测试用例
    public static void main(String[] args) {
        // IPv4测试
        String ipv4 = "192.168.1.1";
        String port1 = "8080";
        System.out.println(concatenateIpAndPort(ipv4, port1));  // 输出: 192.168.1.1:8080

        // IPv6测试
        String ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        String port2 = "80";
        System.out.println(concatenateIpAndPort(ipv6, port2));  // 输出: [2001:0db8:85a3:0000:0000:8a2e:0370:7334]:80

        // 压缩格式IPv6测试
        String ipv6Compressed = "2001:db8::1";
        System.out.println(concatenateIpAndPort(ipv6Compressed, port2));  // 输出: [2001:db8::1]:80

        // 无效IP测试
        try {
            System.out.println(concatenateIpAndPort("invalid.ip", "1234"));
        } catch (IllegalArgumentException e) {
            System.out.println("捕获到预期异常: " + e.getMessage());
        }

        // 无效端口测试 - 非数字
        try {
            System.out.println(concatenateIpAndPort(ipv4, "abc"));
        } catch (IllegalArgumentException e) {
            System.out.println("捕获到预期异常: " + e.getMessage());
        }

        // 无效端口测试 - 超出范围
        try {
            System.out.println(concatenateIpAndPort(ipv4, "70000"));
        } catch (IllegalArgumentException e) {
            System.out.println("捕获到预期异常: " + e.getMessage());
        }

        // 空端口测试
        try {
            System.out.println(concatenateIpAndPort(ipv4, ""));
        } catch (IllegalArgumentException e) {
            System.out.println("捕获到预期异常: " + e.getMessage());
        }
    }
}