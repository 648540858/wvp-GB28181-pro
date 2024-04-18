package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 终端参数设置
 */
@Schema(description = "JT终端参数设置")
public class JTDeviceConfig {

    @ConfigAttribute(id = 0x1, description = "终端心跳发送间隔 ,单位为秒(s)")
    private Long keepaliveInterval;

    @ConfigAttribute(id = 0x2, description = "TCP消息应答超时时间 ,单位为秒(s)")
    private Long tcpResponseTimeout;

    @ConfigAttribute(id = 0x3, description = "TCP消息重传次数")
    private Long tcpRetransmissionCount;

    @ConfigAttribute(id = 0x4, description = "UDP消息应答超时时间 ,单位为秒(s)")
    private Long udpResponseTimeout;

    @ConfigAttribute(id = 0x5, description = "UDP消息重传次数")
    private Long udpRetransmissionCount;

    @ConfigAttribute(id = 0x6, description = "SMS 消息应答超时时间 ,单位为秒(s)")
    private Long smsResponseTimeout;

    @ConfigAttribute(id = 0x7, description = "SMS 消息重传次数")
    private Long smsRetransmissionCount;

    @ConfigAttribute(id = 0x10, description = "主服务器 APN,无线通信拨号访问点 。若网络制式为 CDMA,则该处 为 PPP拨号号码")
    private String apnMaster;

    @ConfigAttribute(id = 0x11, description = "主服务器无线通信拨号用户名")
    private String dialingUsernameMaster;

    @ConfigAttribute(id = 0x12, description = "主服务器无线通信拨号密码")
    private String dialingPasswordMaster;

    @ConfigAttribute(id = 0x13, description = "主服务器地址 !IP或域名 ! 以冒号分割主机和端口 !多个服务器使用 分号分割")
    private String addressMaster;

    @ConfigAttribute(id = 0x14, description = "备份服务器 APN")
    private String apnBackup;

    @ConfigAttribute(id = 0x15, description = "备份服务器无线通信拨号用户名")
    private String dialingUsernameBackup;

    @ConfigAttribute(id = 0x16, description = "备份服务器无线通信拨号密码")
    private String dialingPasswordBackup;

    @ConfigAttribute(id = 0x17, description = "备用服务器备份地址 !IP或域名 ! 以冒号分割主机和端口 !多个服务 器使用分号分割")
    private String addressBackup;

    @ConfigAttribute(id = 0x1a, description = "道路运输证 IC卡认证主服务器 IP地址或域名")
    private String addressIcMaster;

    @ConfigAttribute(id = 0x1b, description = "道路运输证 IC卡认证主服务器 TCP端口")
    private Long tcpPortIcMaster;

    @ConfigAttribute(id = 0x1c, description = "道路运输证 IC卡认证主服务器 UDP端口")
    private Long udpPortIcMaster;

    @ConfigAttribute(id = 0x1d, description = "道路运输证 IC卡认证备份服务器 IP地址或域名 !端口同主服务器")
    private String addressIcBackup;

    @ConfigAttribute(id = 0x20, description = "位置汇报策略 0定时汇报,1定距汇报,2定时和定距汇报")
    private Long locationReportingStrategy;

    @ConfigAttribute(id = 0x21, description = "位置汇报方案 0根据ACC状态, 1根据登录状态和ACC状态,先判断登录状态,若登录再根据ACC状态")
    private Long locationReportingPlan;

    @ConfigAttribute(id = 0x22, description = "驾驶员未登录汇报时间间隔,单位为秒,值大于零")
    private Long reportingIntervalOffline;

    @ConfigAttribute(id = 0x23, description = "从服务器 APN# 该值为空时 !终端应使用主服务器相同配置")
    private String apnSlave;

    @ConfigAttribute(id = 0x24, description = "从服务器无线通信拨号密码 #  该值为空时 !终端应使用主服务器相 同配置")
    private String dialingUsernameSlave;

    @ConfigAttribute(id = 0x25, description = "从服务器备份地址 IP或域名 !主机和端口用冒号分割 !多个服务器 使用分号分割")
    private String dialingPasswordSlave;

    @ConfigAttribute(id = 0x26, description = "从服务器备份地址 IP或域名 !主机和端口用冒号分割 !多个服务器 使用分号分割")
    private String addressSlave;

    @ConfigAttribute(id = 0x27, description = "休眠时汇报时间间隔 单位为秒 值大于0")
    private Long reportingIntervalDormancy;

    @ConfigAttribute(id = 0x28, description = "紧急报警时汇报时间间隔 单位为秒 值大于0")
    private Long reportingIntervalEmergencyAlarm;

    // TODO 未完待续

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

    public String getApnMaster() {
        return apnMaster;
    }

    public void setApnMaster(String apnMaster) {
        this.apnMaster = apnMaster;
    }

    public String getDialingUsernameMaster() {
        return dialingUsernameMaster;
    }

    public void setDialingUsernameMaster(String dialingUsernameMaster) {
        this.dialingUsernameMaster = dialingUsernameMaster;
    }

    public String getDialingPasswordMaster() {
        return dialingPasswordMaster;
    }

    public void setDialingPasswordMaster(String dialingPasswordMaster) {
        this.dialingPasswordMaster = dialingPasswordMaster;
    }

    public String getAddressMaster() {
        return addressMaster;
    }

    public void setAddressMaster(String addressMaster) {
        this.addressMaster = addressMaster;
    }

    public String getApnBackup() {
        return apnBackup;
    }

    public void setApnBackup(String apnBackup) {
        this.apnBackup = apnBackup;
    }

    public String getDialingUsernameBackup() {
        return dialingUsernameBackup;
    }

    public void setDialingUsernameBackup(String dialingUsernameBackup) {
        this.dialingUsernameBackup = dialingUsernameBackup;
    }

    public String getDialingPasswordBackup() {
        return dialingPasswordBackup;
    }

    public void setDialingPasswordBackup(String dialingPasswordBackup) {
        this.dialingPasswordBackup = dialingPasswordBackup;
    }

    public String getAddressBackup() {
        return addressBackup;
    }

    public void setAddressBackup(String addressBackup) {
        this.addressBackup = addressBackup;
    }

    public String getAddressIcMaster() {
        return addressIcMaster;
    }

    public void setAddressIcMaster(String addressIcMaster) {
        this.addressIcMaster = addressIcMaster;
    }

    public Long getTcpPortIcMaster() {
        return tcpPortIcMaster;
    }

    public void setTcpPortIcMaster(Long tcpPortIcMaster) {
        this.tcpPortIcMaster = tcpPortIcMaster;
    }

    public Long getUdpPortIcMaster() {
        return udpPortIcMaster;
    }

    public void setUdpPortIcMaster(Long udpPortIcMaster) {
        this.udpPortIcMaster = udpPortIcMaster;
    }

    public String getAddressIcBackup() {
        return addressIcBackup;
    }

    public void setAddressIcBackup(String addressIcBackup) {
        this.addressIcBackup = addressIcBackup;
    }

    public Long getLocationReportingStrategy() {
        return locationReportingStrategy;
    }

    public void setLocationReportingStrategy(Long locationReportingStrategy) {
        this.locationReportingStrategy = locationReportingStrategy;
    }

    public Long getLocationReportingPlan() {
        return locationReportingPlan;
    }

    public void setLocationReportingPlan(Long locationReportingPlan) {
        this.locationReportingPlan = locationReportingPlan;
    }

    public Long getReportingIntervalOffline() {
        return reportingIntervalOffline;
    }

    public void setReportingIntervalOffline(Long reportingIntervalOffline) {
        this.reportingIntervalOffline = reportingIntervalOffline;
    }

    public String getApnSlave() {
        return apnSlave;
    }

    public void setApnSlave(String apnSlave) {
        this.apnSlave = apnSlave;
    }

    public String getDialingUsernameSlave() {
        return dialingUsernameSlave;
    }

    public void setDialingUsernameSlave(String dialingUsernameSlave) {
        this.dialingUsernameSlave = dialingUsernameSlave;
    }

    public String getDialingPasswordSlave() {
        return dialingPasswordSlave;
    }

    public void setDialingPasswordSlave(String dialingPasswordSlave) {
        this.dialingPasswordSlave = dialingPasswordSlave;
    }

    public String getAddressSlave() {
        return addressSlave;
    }

    public void setAddressSlave(String addressSlave) {
        this.addressSlave = addressSlave;
    }

    public Long getReportingIntervalDormancy() {
        return reportingIntervalDormancy;
    }

    public void setReportingIntervalDormancy(Long reportingIntervalDormancy) {
        this.reportingIntervalDormancy = reportingIntervalDormancy;
    }

    public Long getReportingIntervalEmergencyAlarm() {
        return reportingIntervalEmergencyAlarm;
    }

    public void setReportingIntervalEmergencyAlarm(Long reportingIntervalEmergencyAlarm) {
        this.reportingIntervalEmergencyAlarm = reportingIntervalEmergencyAlarm;
    }

    @Override
    public String toString() {
        return "JTDeviceConfig{" +
                "keepaliveInterval=" + keepaliveInterval +
                ", tcpResponseTimeout=" + tcpResponseTimeout +
                ", tcpRetransmissionCount=" + tcpRetransmissionCount +
                ", udpResponseTimeout=" + udpResponseTimeout +
                ", udpRetransmissionCount=" + udpRetransmissionCount +
                ", smsResponseTimeout=" + smsResponseTimeout +
                ", smsRetransmissionCount=" + smsRetransmissionCount +
                ", apnMaster='" + apnMaster + '\'' +
                ", dialingUsernameMaster='" + dialingUsernameMaster + '\'' +
                ", dialingPasswordMaster='" + dialingPasswordMaster + '\'' +
                ", addressMaster='" + addressMaster + '\'' +
                ", apnBackup='" + apnBackup + '\'' +
                ", dialingUsernameBackup='" + dialingUsernameBackup + '\'' +
                ", dialingPasswordBackup='" + dialingPasswordBackup + '\'' +
                ", addressBackup='" + addressBackup + '\'' +
                ", addressIcMaster='" + addressIcMaster + '\'' +
                ", tcpPortIcMaster=" + tcpPortIcMaster +
                ", udpPortIcMaster=" + udpPortIcMaster +
                ", addressIcBackup='" + addressIcBackup + '\'' +
                ", locationReportingStrategy=" + locationReportingStrategy +
                ", locationReportingPlan=" + locationReportingPlan +
                ", reportingIntervalOffline=" + reportingIntervalOffline +
                ", apnSlave='" + apnSlave + '\'' +
                ", dialingUsernameSlave='" + dialingUsernameSlave + '\'' +
                ", dialingPasswordSlave='" + dialingPasswordSlave + '\'' +
                ", addressSlave='" + addressSlave + '\'' +
                ", reportingIntervalDormancy=" + reportingIntervalDormancy +
                ", reportingIntervalEmergencyAlarm=" + reportingIntervalEmergencyAlarm +
                '}';
    }
}
