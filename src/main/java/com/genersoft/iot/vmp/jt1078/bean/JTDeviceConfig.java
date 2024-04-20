package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.IllegalDrivingPeriods;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 终端参数设置
 */
@Schema(description = "JT终端参数设置")
public class JTDeviceConfig {

    @ConfigAttribute(id = 0x1, type="Long", description = "终端心跳发送间隔,单位为秒(s)")
    private Long keepaliveInterval;

    @ConfigAttribute(id = 0x2, type="Long", description = "TCP消息应答超时时间,单位为秒(s)")
    private Long tcpResponseTimeout;

    @ConfigAttribute(id = 0x3, type="Long", description = "TCP消息重传次数")
    private Long tcpRetransmissionCount;

    @ConfigAttribute(id = 0x4, type="Long", description = "UDP消息应答超时时间,单位为秒(s)")
    private Long udpResponseTimeout;

    @ConfigAttribute(id = 0x5, type="Long", description = "UDP消息重传次数")
    private Long udpRetransmissionCount;

    @ConfigAttribute(id = 0x6, type="Long", description = "SMS 消息应答超时时间,单位为秒(s)")
    private Long smsResponseTimeout;

    @ConfigAttribute(id = 0x7, type="Long", description = "SMS 消息重传次数")
    private Long smsRetransmissionCount;

    @ConfigAttribute(id = 0x10, type="String", description = "主服务器APN无线通信拨号访问点,若网络制式为 CDMA,则该处 为 PPP拨号号码")
    private String apnMaster;

    @ConfigAttribute(id = 0x11, type="String", description = "主服务器无线通信拨号用户名")
    private String dialingUsernameMaster;

    @ConfigAttribute(id = 0x12, type="String", description = "主服务器无线通信拨号密码")
    private String dialingPasswordMaster;

    @ConfigAttribute(id = 0x13, type="String", description = "主服务器地址IP或域名,以冒号分割主机和端口 多个服务器使用分号分割")
    private String addressMaster;

    @ConfigAttribute(id = 0x14, type="String", description = "备份服务器APN")
    private String apnBackup;

    @ConfigAttribute(id = 0x15, type="String", description = "备份服务器无线通信拨号用户名")
    private String dialingUsernameBackup;

    @ConfigAttribute(id = 0x16, type="String", description = "备份服务器无线通信拨号密码")
    private String dialingPasswordBackup;

    @ConfigAttribute(id = 0x17, type="String", description = "备用服务器备份地址IP或域名,以冒号分割主机和端口 多个服务器使用分号分割")
    private String addressBackup;

    @ConfigAttribute(id = 0x1a, type="String", description = "道路运输证IC卡认证主服务器IP地址或域名")
    private String addressIcMaster;

    @ConfigAttribute(id = 0x1b, type="Long", description = "道路运输证IC卡认证主服务器TCP端口")
    private Long tcpPortIcMaster;

    @ConfigAttribute(id = 0x1c, type="Long", description = "道路运输证IC卡认证主服务器UDP端口")
    private Long udpPortIcMaster;

    @ConfigAttribute(id = 0x1d, type="Long", description = "道路运输证IC卡认证备份服务器IP地址或域名,端口同主服务器")
    private String addressIcBackup;

    @ConfigAttribute(id = 0x20, type="Long", description = "位置汇报策略, 0定时汇报 1定距汇报 2定时和定距汇报")
    private Long locationReportingStrategy;

    @ConfigAttribute(id = 0x21, type="Long", description = "位置汇报方案,0根据ACC状态 1根据登录状态和ACC状态,先判断登录状态,若登录再根据ACC状态")
    private Long locationReportingPlan;

    @ConfigAttribute(id = 0x22, type="Long", description = "驾驶员未登录汇报时间间隔,单位为秒,值大于零")
    private Long reportingIntervalOffline;

    @ConfigAttribute(id = 0x23, type="String", description = "从服务器 APN# 该值为空时 !终端应使用主服务器相同配置")
    private String apnSlave;

    @ConfigAttribute(id = 0x24, type="String", description = "从服务器无线通信拨号密码 #  该值为空时 !终端应使用主服务器相 同配置")
    private String dialingUsernameSlave;

    @ConfigAttribute(id = 0x25, type="String", description = "从服务器备份地址 IP或域名 !主机和端口用冒号分割 !多个服务器 使用分号分割")
    private String dialingPasswordSlave;

    @ConfigAttribute(id = 0x26, type="String", description = "从服务器备份地址 IP或域名 !主机和端口用冒号分割 !多个服务器 使用分号分割")
    private String addressSlave;

    @ConfigAttribute(id = 0x27, type="Long", description = "休眠时汇报时间间隔 单位为秒 值大于0")
    private Long reportingIntervalDormancy;

    @ConfigAttribute(id = 0x28, type="Long", description = "紧急报警时汇报时间间隔 单位为秒 值大于0")
    private Long reportingIntervalEmergencyAlarm;

    @ConfigAttribute(id = 0x29, type="Long", description = "缺省时间汇报间隔 单位为秒 值大于0")
    private Long reportingIntervalDefault;

    @ConfigAttribute(id = 0x2c, type="Long", description = "缺省距离汇报间隔 单位为米 值大于0")
    private Long reportingDistanceDefault;

    @ConfigAttribute(id = 0x2d, type="Long", description = "驾驶员未登录汇报距离间隔 单位为米 值大于0")
    private Long reportingDistanceOffline;

    @ConfigAttribute(id = 0x2e, type="Long", description = "休眠时汇报距离间隔 单位为米 值大于0")
    private Long reportingDistanceDormancy;

    @ConfigAttribute(id = 0x2f, type="Long", description = "紧急报警时汇报距离间隔 单位为米 值大于0")
    private Long reportingDistanceEmergencyAlarm;

    @ConfigAttribute(id = 0x30, type="Long", description = "拐点补传角度 ,值小于180")
    private Long inflectionPointAngle;

    @ConfigAttribute(id = 0x31, type="Long", description = "电子围栏半径(非法位移國值) ,单位为米(m)")
    private Integer fenceRadius;

    @ConfigAttribute(id = 0x32, type="IllegalDrivingPeriods", description = "违规行驶时段范围 ,精确到分")
    private IllegalDrivingPeriods illegalDrivingPeriods;

    @ConfigAttribute(id = 0x40, type="String", description = "监控平台电话号码")
    private String platformPhoneNumber;

    @ConfigAttribute(id = 0x41, type="String", description = "复位电话号码 ,可采用此电话号码拨打终端电话让终端复位")
    private String phoneNumberForReset;

    @ConfigAttribute(id = 0x42, type="String", description = "恢复出厂设置电话号码 ,可采用此电话号码拨打终端电话让终端恢 复出厂设置")
    private String phoneNumberForFactoryReset;

    @ConfigAttribute(id = 0x42, type="String", description = "监控平台 SMS 电话号码")
    private String phoneNumberForSms;

    @ConfigAttribute(id = 0x44, type="String", description = "接收终端 SMS 文本报警号码")
    private String phoneNumberForReceiveTextAlarm;

    @ConfigAttribute(id = 0x45, type="Long", description = "终端电话接听策略 。0:自动接听；1:ACCON时自动接听 ,OFF时手动接听")
    private Long phoneAnsweringPolicy;

    @ConfigAttribute(id = 0x46, type="Long", description = "每次最长通话时间 ,单位为秒(s) ,0 为不允许通话 ,0xFFFFFFFF为不限制")
    private Long longestCallTimeForPerSession;

    @ConfigAttribute(id = 0x47, type="Long", description = "当月最长通话时间 ,单位为秒(s) ,0 为不允许通话 ,0xFFFFFFFF为 不限制")
    private Long longestCallTimeInMonth;

    @ConfigAttribute(id = 0x48, type="String", description = "监听电话号码")
    private String phoneNumbersForListen;

    @ConfigAttribute(id = 0x49, type="String", description = "监管平台特权短信号码")
    private String privilegedSMSNumber;

    @ConfigAttribute(id = 0x50, type="Long", description = "报警屏蔽字 ,与位置信息汇报消息中的报警标志相对应 ,相应位为 1 则相应报警被屏蔽")
    private Long alarmMaskingWord;

    @ConfigAttribute(id = 0x51, type="Long", description = "报警发送文本 SMS 开关 , 与位置信息汇报消息中的报警标志相对 应 ,相应位为1 则相应报警时发送文本 SMS")
    private Long alarmSendsTextSmsSwitch;

    @ConfigAttribute(id = 0x52, type="Long", description = "报警拍摄开关 ,与位置信息汇报消息中的报警标志相对应 ,相应位为 1 则相应报警时摄像头拍摄")
    private Long alarmShootingSwitch;

    @ConfigAttribute(id = 0x53, type="Long", description = "报警拍摄存储标志 ,与位置信息汇报消息中的报警标志相对应 ,相应 位为1 则对相应报警时拍的照片进行存储 ,否则实时上传")
    private Long alarmShootingStorageFlags;

    @ConfigAttribute(id = 0x54, type="Long", description = "关键标志 ,与位置信息汇报消息中的报警标志相对应 ,相应位为 1 则 对相应报警为关键报警")
    private Long KeySign;

    @ConfigAttribute(id = 0x55, type="Long", description = "最高速度 ,单位为千米每小时(km/h)")
    private Long topSpeed;

    @ConfigAttribute(id = 0x56, type="Long", description = "超速持续时间 ,单位为秒(s)")
    private Long overSpeedDuration;

    @ConfigAttribute(id = 0x57, type="Long", description = "连续驾驶时间门限 单位为秒(s)")
    private Long continuousDrivingTimeThreshold;

    @ConfigAttribute(id = 0x58, type="Long", description = "当天累计驾驶时间门限 单位为秒(s)")
    private Long cumulativeDrivingTimeThresholdForTheDay;

    @ConfigAttribute(id = 0x59, type="Long", description = "最小休息时间 单位为秒(s)")
    private Long minimumBreakTime;

    @ConfigAttribute(id = 0x5a, type="Long", description = "最长停车时间 单位为秒(s)")
    private Long maximumParkingTime;

    @ConfigAttribute(id = 0x5b, type="Long", description = "超速预警差值 单位为1/10 千米每小时(1/10km/h)")
    private Long overSpeedWarningDifference;

    @ConfigAttribute(id = 0x5c, type="Long", description = "疲劳驾驶预警差值 单位为秒 值大于零")
    private Long drowsyDrivingWarningDifference;



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

    public Long getReportingIntervalDefault() {
        return reportingIntervalDefault;
    }

    public void setReportingIntervalDefault(Long reportingIntervalDefault) {
        this.reportingIntervalDefault = reportingIntervalDefault;
    }

    public Long getReportingDistanceDefault() {
        return reportingDistanceDefault;
    }

    public void setReportingDistanceDefault(Long reportingDistanceDefault) {
        this.reportingDistanceDefault = reportingDistanceDefault;
    }

    public Long getReportingDistanceOffline() {
        return reportingDistanceOffline;
    }

    public void setReportingDistanceOffline(Long reportingDistanceOffline) {
        this.reportingDistanceOffline = reportingDistanceOffline;
    }

    public Long getReportingDistanceDormancy() {
        return reportingDistanceDormancy;
    }

    public void setReportingDistanceDormancy(Long reportingDistanceDormancy) {
        this.reportingDistanceDormancy = reportingDistanceDormancy;
    }

    public Long getReportingDistanceEmergencyAlarm() {
        return reportingDistanceEmergencyAlarm;
    }

    public void setReportingDistanceEmergencyAlarm(Long reportingDistanceEmergencyAlarm) {
        this.reportingDistanceEmergencyAlarm = reportingDistanceEmergencyAlarm;
    }

    public Long getInflectionPointAngle() {
        return inflectionPointAngle;
    }

    public void setInflectionPointAngle(Long inflectionPointAngle) {
        this.inflectionPointAngle = inflectionPointAngle;
    }

    public Integer getFenceRadius() {
        return fenceRadius;
    }

    public void setFenceRadius(Integer fenceRadius) {
        this.fenceRadius = fenceRadius;
    }

    public IllegalDrivingPeriods getIllegalDrivingPeriods() {
        return illegalDrivingPeriods;
    }

    public void setIllegalDrivingPeriods(IllegalDrivingPeriods illegalDrivingPeriods) {
        this.illegalDrivingPeriods = illegalDrivingPeriods;
    }

    public String getPlatformPhoneNumber() {
        return platformPhoneNumber;
    }

    public void setPlatformPhoneNumber(String platformPhoneNumber) {
        this.platformPhoneNumber = platformPhoneNumber;
    }

    public String getPhoneNumberForReset() {
        return phoneNumberForReset;
    }

    public void setPhoneNumberForReset(String phoneNumberForReset) {
        this.phoneNumberForReset = phoneNumberForReset;
    }

    public String getPhoneNumberForFactoryReset() {
        return phoneNumberForFactoryReset;
    }

    public void setPhoneNumberForFactoryReset(String phoneNumberForFactoryReset) {
        this.phoneNumberForFactoryReset = phoneNumberForFactoryReset;
    }

    public String getPhoneNumberForSms() {
        return phoneNumberForSms;
    }

    public void setPhoneNumberForSms(String phoneNumberForSms) {
        this.phoneNumberForSms = phoneNumberForSms;
    }

    public String getPhoneNumberForReceiveTextAlarm() {
        return phoneNumberForReceiveTextAlarm;
    }

    public void setPhoneNumberForReceiveTextAlarm(String phoneNumberForReceiveTextAlarm) {
        this.phoneNumberForReceiveTextAlarm = phoneNumberForReceiveTextAlarm;
    }

    public Long getPhoneAnsweringPolicy() {
        return phoneAnsweringPolicy;
    }

    public void setPhoneAnsweringPolicy(Long phoneAnsweringPolicy) {
        this.phoneAnsweringPolicy = phoneAnsweringPolicy;
    }

    public Long getLongestCallTimeForPerSession() {
        return longestCallTimeForPerSession;
    }

    public void setLongestCallTimeForPerSession(Long longestCallTimeForPerSession) {
        this.longestCallTimeForPerSession = longestCallTimeForPerSession;
    }

    public Long getLongestCallTimeInMonth() {
        return longestCallTimeInMonth;
    }

    public void setLongestCallTimeInMonth(Long longestCallTimeInMonth) {
        this.longestCallTimeInMonth = longestCallTimeInMonth;
    }

    public String getPhoneNumbersForListen() {
        return phoneNumbersForListen;
    }

    public void setPhoneNumbersForListen(String phoneNumbersForListen) {
        this.phoneNumbersForListen = phoneNumbersForListen;
    }

    public String getPrivilegedSMSNumber() {
        return privilegedSMSNumber;
    }

    public void setPrivilegedSMSNumber(String privilegedSMSNumber) {
        this.privilegedSMSNumber = privilegedSMSNumber;
    }

    public Long getAlarmMaskingWord() {
        return alarmMaskingWord;
    }

    public void setAlarmMaskingWord(Long alarmMaskingWord) {
        this.alarmMaskingWord = alarmMaskingWord;
    }

    public Long getAlarmSendsTextSmsSwitch() {
        return alarmSendsTextSmsSwitch;
    }

    public void setAlarmSendsTextSmsSwitch(Long alarmSendsTextSmsSwitch) {
        this.alarmSendsTextSmsSwitch = alarmSendsTextSmsSwitch;
    }

    public Long getAlarmShootingSwitch() {
        return alarmShootingSwitch;
    }

    public void setAlarmShootingSwitch(Long alarmShootingSwitch) {
        this.alarmShootingSwitch = alarmShootingSwitch;
    }

    public Long getAlarmShootingStorageFlags() {
        return alarmShootingStorageFlags;
    }

    public void setAlarmShootingStorageFlags(Long alarmShootingStorageFlags) {
        this.alarmShootingStorageFlags = alarmShootingStorageFlags;
    }

    public Long getKeySign() {
        return KeySign;
    }

    public void setKeySign(Long keySign) {
        KeySign = keySign;
    }

    public Long getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(Long topSpeed) {
        this.topSpeed = topSpeed;
    }

    public Long getOverSpeedDuration() {
        return overSpeedDuration;
    }

    public void setOverSpeedDuration(Long overSpeedDuration) {
        this.overSpeedDuration = overSpeedDuration;
    }

    public Long getContinuousDrivingTimeThreshold() {
        return continuousDrivingTimeThreshold;
    }

    public void setContinuousDrivingTimeThreshold(Long continuousDrivingTimeThreshold) {
        this.continuousDrivingTimeThreshold = continuousDrivingTimeThreshold;
    }

    public Long getCumulativeDrivingTimeThresholdForTheDay() {
        return cumulativeDrivingTimeThresholdForTheDay;
    }

    public void setCumulativeDrivingTimeThresholdForTheDay(Long cumulativeDrivingTimeThresholdForTheDay) {
        this.cumulativeDrivingTimeThresholdForTheDay = cumulativeDrivingTimeThresholdForTheDay;
    }

    public Long getMinimumBreakTime() {
        return minimumBreakTime;
    }

    public void setMinimumBreakTime(Long minimumBreakTime) {
        this.minimumBreakTime = minimumBreakTime;
    }

    public Long getMaximumParkingTime() {
        return maximumParkingTime;
    }

    public void setMaximumParkingTime(Long maximumParkingTime) {
        this.maximumParkingTime = maximumParkingTime;
    }

    public Long getOverSpeedWarningDifference() {
        return overSpeedWarningDifference;
    }

    public void setOverSpeedWarningDifference(Long overSpeedWarningDifference) {
        this.overSpeedWarningDifference = overSpeedWarningDifference;
    }

    public Long getDrowsyDrivingWarningDifference() {
        return drowsyDrivingWarningDifference;
    }

    public void setDrowsyDrivingWarningDifference(Long drowsyDrivingWarningDifference) {
        this.drowsyDrivingWarningDifference = drowsyDrivingWarningDifference;
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
