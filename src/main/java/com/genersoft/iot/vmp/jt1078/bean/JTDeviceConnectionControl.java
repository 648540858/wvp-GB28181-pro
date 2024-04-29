package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.CameraTimer;
import com.genersoft.iot.vmp.jt1078.bean.config.CollisionAlarmParams;
import com.genersoft.iot.vmp.jt1078.bean.config.GnssPositioningMode;
import com.genersoft.iot.vmp.jt1078.bean.config.IllegalDrivingPeriods;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 终端控制
 */
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

    public Boolean getSwitchOn() {
        return switchOn;
    }

    public void setSwitchOn(Boolean switchOn) {
        this.switchOn = switchOn;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(Integer tcpPort) {
        this.tcpPort = tcpPort;
    }

    public Integer getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(Integer udpPort) {
        this.udpPort = udpPort;
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Long timeLimit) {
        this.timeLimit = timeLimit;
    }

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
