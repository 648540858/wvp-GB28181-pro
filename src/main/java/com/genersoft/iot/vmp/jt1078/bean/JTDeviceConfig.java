package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 终端参数设置
 */
@Schema(description = "JT终端参数设置")
public class JTDeviceConfig {

    @ConfigAttribute(id = 1, description = "终端心跳发送间隔 ,单位为秒(s)")
    private Long keepaliveInterval;

    @ConfigAttribute(id = 2, description = "TCP消息应答超时时间 ,单位为秒(s)")
    private Long tcpResponseTimeout;

    @ConfigAttribute(id = 3, description = "TCP消息重传次数")
    private Long tcpRetransmissionCount;

    @ConfigAttribute(id = 4, description = "UDP消息应答超时时间 ,单位为秒(s)")
    private Long udpResponseTimeout;

    @ConfigAttribute(id = 5, description = "UDP消息重传次数")
    private Long udpRetransmissionCount;

    @ConfigAttribute(id = 6, description = "SMS 消息应答超时时间 ,单位为秒(s)")
    private Long smsResponseTimeout;

    @ConfigAttribute(id = 7, description = "SMS 消息重传次数")
    private Long smsRetransmissionCount;

    @ConfigAttribute(id = 10, description = "主服务器 APN,无线通信拨号访问点 。若网络制式为 CDMA,则该处 为 PPP拨号号码")
    private String dialingNumber;

    @ConfigAttribute(id = 11, description = "主服务器无线通信拨号用户名")
    private String dialingUsernameMain;

    @ConfigAttribute(id = 12, description = "主服务器无线通信拨号密码")
    private String dialingPasswordMain;

    @ConfigAttribute(id = 13, description = "主服务器地址 !IP或域名 ! 以冒号分割主机和端口 !多个服务器使用 分号分割")
    private String addressMain;

    public Long getKeepaliveInterval() {
        return keepaliveInterval;
    }

    public void setKeepaliveInterval(Long keepaliveInterval) {
        this.keepaliveInterval = keepaliveInterval;
    }

    public Long getTcpResponseTimeout() {
        return tcpResponseTimeout;
    }

    public void setTcpResponseTimeout(Long tcpResponseTimeout) {
        this.tcpResponseTimeout = tcpResponseTimeout;
    }

    public Long getTcpRetransmissionCount() {
        return tcpRetransmissionCount;
    }

    public void setTcpRetransmissionCount(Long tcpRetransmissionCount) {
        this.tcpRetransmissionCount = tcpRetransmissionCount;
    }

    public Long getUdpResponseTimeout() {
        return udpResponseTimeout;
    }

    public void setUdpResponseTimeout(Long udpResponseTimeout) {
        this.udpResponseTimeout = udpResponseTimeout;
    }

    public Long getUdpRetransmissionCount() {
        return udpRetransmissionCount;
    }

    public void setUdpRetransmissionCount(Long udpRetransmissionCount) {
        this.udpRetransmissionCount = udpRetransmissionCount;
    }

    public Long getSmsResponseTimeout() {
        return smsResponseTimeout;
    }

    public void setSmsResponseTimeout(Long smsResponseTimeout) {
        this.smsResponseTimeout = smsResponseTimeout;
    }

    public Long getSmsRetransmissionCount() {
        return smsRetransmissionCount;
    }

    public void setSmsRetransmissionCount(Long smsRetransmissionCount) {
        this.smsRetransmissionCount = smsRetransmissionCount;
    }

    public String getDialingNumber() {
        return dialingNumber;
    }

    public void setDialingNumber(String dialingNumber) {
        this.dialingNumber = dialingNumber;
    }

    public String getDialingUsernameMain() {
        return dialingUsernameMain;
    }

    public void setDialingUsernameMain(String dialingUsernameMain) {
        this.dialingUsernameMain = dialingUsernameMain;
    }

    public String getDialingPasswordMain() {
        return dialingPasswordMain;
    }

    public void setDialingPasswordMain(String dialingPasswordMain) {
        this.dialingPasswordMain = dialingPasswordMain;
    }

    public String getAddressMain() {
        return addressMain;
    }

    public void setAddressMain(String addressMain) {
        this.addressMain = addressMain;
    }
}
