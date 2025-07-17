package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JT 终端控制
 */
@Data
@Schema(description = "终端控制")
public class JTDeviceConnectionControl {

    /**
     * false 表示切换到指定监管平台服务器 ,true 表示切换回原 缺省监控平台服务器
     */
    private Boolean switchOn;
    /**
     * 监管平台鉴权码
     */
    private String authentication;

    /**
     * 拨号点名称
     */
    private String name;

    /**
     * 拨号用户名
     */
    private String username;

    /**
     * 拨号密码
     */
    private String password;

    /**
     * 地址
     */
    private String address;

    /**
     * TCP端口
     */
    private Integer tcpPort;

    /**
     * UDP端口
     */
    private Integer udpPort;

    /**
     * 连接到指定服务器时限
     */
    private Long timeLimit;

    @Override
    public String toString() {
        return "JTDeviceConnectionControl{" +
                "switchOn=" + switchOn +
                ", authentication='" + authentication + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", address='" + address + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", timeLimit=" + timeLimit +
                '}';
    }
}
