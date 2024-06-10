package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.*;
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

    @ConfigAttribute(id = 0x1d, type="String", description = "道路运输证IC卡认证备份服务器IP地址或域名,端口同主服务器")
    private String addressIcBackup;

    @ConfigAttribute(id = 0x20, type="Long", description = "位置汇报策略, 0定时汇报 1定距汇报 2定时和定距汇报")
    private Long locationReportingStrategy;

    @ConfigAttribute(id = 0x21, type="Long", description = "位置汇报方案,0根据ACC状态 1根据登录状态和ACC状态,先判断登录状态,若登录再根据ACC状态")
    private Long locationReportingPlan;

    @ConfigAttribute(id = 0x22, type="Long", description = "驾驶员未登录汇报时间间隔,单位为秒,值大于零")
    private Long reportingIntervalOffline;

    @ConfigAttribute(id = 0x23, type="String", description = "从服务器 APN# 该值为空时 !终端应使用主服务器相同配置")
    private String apnSlave;

    @ConfigAttribute(id = 0x24, type="String", description = "从服务器无线通信拨号用户名 #  该值为空时 !终端应使用主服务器 相同配置")
    private String dialingUsernameSlave;

    @ConfigAttribute(id = 0x25, type="String", description = "从服务器无线通信拨号密码 #  该值为空时 !终端应使用主服务器相 同配置")
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

    @ConfigAttribute(id = 0x31, type="Integer", description = "电子围栏半径(非法位移國值) ,单位为米(m)")
    private Integer fenceRadius;

    @ConfigAttribute(id = 0x32, type="IllegalDrivingPeriods", description = "违规行驶时段范围 ,精确到分")
    private JTIllegalDrivingPeriods illegalDrivingPeriods;

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

    @ConfigAttribute(id = 0x45, type="Long", description = "终端电话接听策略 。0:自动接听；1:ACC ON时自动接听 ,OFF时手动接听")
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
    private Long maxSpeed;

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

    @ConfigAttribute(id = 0x5b, type="Integer", description = "超速预警差值 单位为1/10 千米每小时(1/10km/h)")
    private Integer overSpeedWarningDifference;

    @ConfigAttribute(id = 0x5c, type="Integer", description = "疲劳驾驶预警差值 单位为秒 值大于零")
    private Integer drowsyDrivingWarningDifference;

    @ConfigAttribute(id = 0x5d, type="CollisionAlarmParams", description = "碰撞报警参数设置")
    private JTCollisionAlarmParams collisionAlarmParams;

    @ConfigAttribute(id = 0x5e, type="Integer", description = "侧翻报警参数设置:侧翻角度,单位为度,默认为30")
    private Integer rolloverAlarm;

    @ConfigAttribute(id = 0x64, type="CameraTimer", description = "定时拍照控制")
    private JTCameraTimer cameraTimer;

    @ConfigAttribute(id = 0x70, type="Long", description = "图像/视频质量 设置范围为1~10 1表示最优质量")
    private Long qualityForVideo;

    @ConfigAttribute(id = 0x71, type="Long", description = "亮度，设置范围为0 ~ 255")
    private Long brightness;

    @ConfigAttribute(id = 0x72, type="Long", description = "对比度，设置范围为0 ~ 127")
    private Long contrastRatio;

    @ConfigAttribute(id = 0x73, type="Long", description = "饱和度，设置范围为0 ~ 127")
    private Long saturation;

    @ConfigAttribute(id = 0x74, type="Long", description = "色度，设置范围为0 ~ 255")
    private Long chroma;

    @ConfigAttribute(id = 0x75, type="VideoParam", description = "音视频参数设置")
    private JTVideoParam videoParam;

    @ConfigAttribute(id = 0x76, type="ChannelListParam", description = "音视频通道列表设置")
    private JTChannelListParam channelListParam;

    @ConfigAttribute(id = 0x77, type="ChannelParam", description = "音视频通道列表设置")
    private JTChannelParam channelParam;

    @ConfigAttribute(id = 0x79, type="AlarmRecordingParam", description = "特殊报警录像参数设置")
    private JTAlarmRecordingParam alarmRecordingParam;

    @ConfigAttribute(id = 0x7a, type="VideoAlarmBit", description = "视频相关报警屏蔽字")
    private JTVideoAlarmBit videoAlarmBit;

    @ConfigAttribute(id = 0x7b, type="AnalyzeAlarmParam", description = "图像分析报警参数设置")
    private JTAnalyzeAlarmParam analyzeAlarmParam;

    @ConfigAttribute(id = 0x7c, type="AwakenParam", description = "终端休眠唤醒模式设置")
    private JTAwakenParam awakenParam;

    @ConfigAttribute(id = 0x80, type="Long", description = "车辆里程表读数，单位'1/10km")
    private Long mileage;

    @ConfigAttribute(id = 0x81, type="Integer", description = "车辆所在的省域ID")
    private Integer provincialId;

    @ConfigAttribute(id = 0x82, type="Integer", description = "车辆所在的市域ID")
    private Integer cityId;

    @ConfigAttribute(id = 0x83, type="String", description = "公安交通管理部门颁发的机动车号牌")
    private String licensePlate;

    @ConfigAttribute(id = 0x84, type="Short", description = "车牌颜色,值按照JT/T697-7.2014中的规定,未上牌车辆填0")
    private Short licensePlateColor;

    @ConfigAttribute(id = 0x90, type="Short", description = "GNSS定位模式")
    private JTGnssPositioningMode gnssPositioningMode;

    @ConfigAttribute(id = 0x91, type="Short", description = "GNSS 波特率,定义如下: 0: 4800, 1:9600,  2：19200, 3:38400,  4:57600, 5:115200")
    private Short gnssBaudRate;

    @ConfigAttribute(id = 0x92, type="Short", description = "GNSS 模块详细定位数据输出频率,定义如下: 0: 500ms, 1:1000ms(默认值),  2：2000ms, 3:3000ms,  4:4000ms")
    private Short gnssOutputFrequency;

    @ConfigAttribute(id = 0x93, type="Long", description = "GNSS 模块详细定位数据采集频率 ,单位为秒(s) ,默认为1")
    private Long gnssCollectionFrequency;

    @ConfigAttribute(id = 0x94, type="Short", description = "GNSS 模块详细定位数据上传方式:,定义如下: " +
            "0: 本地存储 ,不上传(默认值) , " +
            "1:按时间间隔上传,  " +
            "2：按距离间隔上传, " +
            "11:按累计时间上传 ,达到传输时间后自动停止上传,  " +
            "12:按累计距离上传 ,达到距离后自动停止上传,  " +
            "13:按累计条数上传 ,达到上传条数后自动停止上传")
    private Short gnssDataUploadMethod;

    @ConfigAttribute(id = 0x95, type="Long", description = "GNSS 模块详细定位数据上传设置:,定义如下: " +
            "1:单位为秒(s),  " +
            "2：单位为米(m) , " +
            "11:单位为 秒(s),  " +
            "12:单位为米(m),  " +
            "13:单位 为条")
    private Long gnssDataUploadMethodUnit;

    @ConfigAttribute(id = 0x100, type="Long", description = "CAN总线通道1 采集时间间隔 ,单位为毫秒(ms) ,0 表示不采集")
    private Long canCollectionTimeForChannel1;

    @ConfigAttribute(id = 0x101, type="Integer", description = "CAN总线通道1 上传时间间隔 ,单位为秒(s) ,0 表示不上传")
    private Integer canUploadIntervalForChannel1;

    @ConfigAttribute(id = 0x102, type="Long", description = "CAN总线通道2 采集时间间隔 ,单位为毫秒(ms) ,0 表示不采集")
    private Long canCollectionTimeForChannel2;

    @ConfigAttribute(id = 0x103, type="Integer", description = "CAN总线通道2 上传时间间隔 ,单位为秒(s) ,0 表示不上传")
    private Integer canUploadIntervalForChannel2;


    public JTAnalyzeAlarmParam getAnalyzeAlarmParam() {
        return analyzeAlarmParam;
    }

    public void setAnalyzeAlarmParam(JTAnalyzeAlarmParam analyzeAlarmParam) {
        this.analyzeAlarmParam = analyzeAlarmParam;
    }

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

    public JTIllegalDrivingPeriods getIllegalDrivingPeriods() {
        return illegalDrivingPeriods;
    }

    public void setIllegalDrivingPeriods(JTIllegalDrivingPeriods illegalDrivingPeriods) {
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

    public Long getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(Long maxSpeed) {
        this.maxSpeed = maxSpeed;
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

    public Integer getOverSpeedWarningDifference() {
        return overSpeedWarningDifference;
    }

    public void setOverSpeedWarningDifference(Integer overSpeedWarningDifference) {
        this.overSpeedWarningDifference = overSpeedWarningDifference;
    }

    public Integer getDrowsyDrivingWarningDifference() {
        return drowsyDrivingWarningDifference;
    }

    public void setDrowsyDrivingWarningDifference(Integer drowsyDrivingWarningDifference) {
        this.drowsyDrivingWarningDifference = drowsyDrivingWarningDifference;
    }

    public JTCollisionAlarmParams getCollisionAlarmParams() {
        return collisionAlarmParams;
    }

    public void setCollisionAlarmParams(JTCollisionAlarmParams collisionAlarmParams) {
        this.collisionAlarmParams = collisionAlarmParams;
    }

    public Integer getRolloverAlarm() {
        return rolloverAlarm;
    }

    public void setRolloverAlarm(Integer rolloverAlarm) {
        this.rolloverAlarm = rolloverAlarm;
    }

    public JTCameraTimer getCameraTimer() {
        return cameraTimer;
    }

    public void setCameraTimer(JTCameraTimer cameraTimer) {
        this.cameraTimer = cameraTimer;
    }

    public Long getQualityForVideo() {
        return qualityForVideo;
    }

    public void setQualityForVideo(Long qualityForVideo) {
        this.qualityForVideo = qualityForVideo;
    }

    public Long getBrightness() {
        return brightness;
    }

    public void setBrightness(Long brightness) {
        this.brightness = brightness;
    }

    public Long getContrastRatio() {
        return contrastRatio;
    }

    public void setContrastRatio(Long contrastRatio) {
        this.contrastRatio = contrastRatio;
    }

    public Long getSaturation() {
        return saturation;
    }

    public void setSaturation(Long saturation) {
        this.saturation = saturation;
    }

    public Long getChroma() {
        return chroma;
    }

    public void setChroma(Long chroma) {
        this.chroma = chroma;
    }

    public Long getMileage() {
        return mileage;
    }

    public void setMileage(Long mileage) {
        this.mileage = mileage;
    }

    public Integer getProvincialId() {
        return provincialId;
    }

    public void setProvincialId(Integer provincialId) {
        this.provincialId = provincialId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Short getLicensePlateColor() {
        return licensePlateColor;
    }

    public void setLicensePlateColor(Short licensePlateColor) {
        this.licensePlateColor = licensePlateColor;
    }

    public JTGnssPositioningMode getGnssPositioningMode() {
        return gnssPositioningMode;
    }

    public void setGnssPositioningMode(JTGnssPositioningMode gnssPositioningMode) {
        this.gnssPositioningMode = gnssPositioningMode;
    }

    public Short getGnssBaudRate() {
        return gnssBaudRate;
    }

    public void setGnssBaudRate(Short gnssBaudRate) {
        this.gnssBaudRate = gnssBaudRate;
    }

    public Short getGnssOutputFrequency() {
        return gnssOutputFrequency;
    }

    public void setGnssOutputFrequency(Short gnssOutputFrequency) {
        this.gnssOutputFrequency = gnssOutputFrequency;
    }

    public Long getGnssCollectionFrequency() {
        return gnssCollectionFrequency;
    }

    public void setGnssCollectionFrequency(Long gnssCollectionFrequency) {
        this.gnssCollectionFrequency = gnssCollectionFrequency;
    }

    public Short getGnssDataUploadMethod() {
        return gnssDataUploadMethod;
    }

    public void setGnssDataUploadMethod(Short gnssDataUploadMethod) {
        this.gnssDataUploadMethod = gnssDataUploadMethod;
    }

    public Long getGnssDataUploadMethodUnit() {
        return gnssDataUploadMethodUnit;
    }

    public void setGnssDataUploadMethodUnit(Long gnssDataUploadMethodUnit) {
        this.gnssDataUploadMethodUnit = gnssDataUploadMethodUnit;
    }

    public Long getCanCollectionTimeForChannel1() {
        return canCollectionTimeForChannel1;
    }

    public void setCanCollectionTimeForChannel1(Long canCollectionTimeForChannel1) {
        this.canCollectionTimeForChannel1 = canCollectionTimeForChannel1;
    }

    public Integer getCanUploadIntervalForChannel1() {
        return canUploadIntervalForChannel1;
    }

    public void setCanUploadIntervalForChannel1(Integer canUploadIntervalForChannel1) {
        this.canUploadIntervalForChannel1 = canUploadIntervalForChannel1;
    }

    public Long getCanCollectionTimeForChannel2() {
        return canCollectionTimeForChannel2;
    }

    public void setCanCollectionTimeForChannel2(Long canCollectionTimeForChannel2) {
        this.canCollectionTimeForChannel2 = canCollectionTimeForChannel2;
    }

    public Integer getCanUploadIntervalForChannel2() {
        return canUploadIntervalForChannel2;
    }

    public void setCanUploadIntervalForChannel2(Integer canUploadIntervalForChannel2) {
        this.canUploadIntervalForChannel2 = canUploadIntervalForChannel2;
    }

    public JTVideoParam getVideoParam() {
        return videoParam;
    }

    public void setVideoParam(JTVideoParam videoParam) {
        this.videoParam = videoParam;
    }

    public JTChannelListParam getChannelListParam() {
        return channelListParam;
    }

    public void setChannelListParam(JTChannelListParam channelListParam) {
        this.channelListParam = channelListParam;
    }

    public JTChannelParam getChannelParam() {
        return channelParam;
    }

    public void setChannelParam(JTChannelParam channelParam) {
        this.channelParam = channelParam;
    }

    public JTAlarmRecordingParam getAlarmRecordingParam() {
        return alarmRecordingParam;
    }

    public void setAlarmRecordingParam(JTAlarmRecordingParam alarmRecordingParam) {
        this.alarmRecordingParam = alarmRecordingParam;
    }

    public JTVideoAlarmBit getVideoAlarmBit() {
        return videoAlarmBit;
    }

    public void setVideoAlarmBit(JTVideoAlarmBit videoAlarmBit) {
        this.videoAlarmBit = videoAlarmBit;
    }


    public JTAwakenParam getAwakenParam() {
        return awakenParam;
    }

    public void setAwakenParam(JTAwakenParam awakenParam) {
        this.awakenParam = awakenParam;
    }

    @Override
    public String toString() {
        return "JTDeviceConfig{" +
                "终端心跳发送间隔： " + keepaliveInterval + "秒" +
                ", TCP消息应答超时时间：" + tcpResponseTimeout + "秒" +
                ", TCP消息重传次数： " + tcpRetransmissionCount + "秒"  +
                ", UDP消息应答超时时间： " + udpResponseTimeout +
                ", UDP消息重传次数： " + udpRetransmissionCount +
                ", SMS 消息应答超时时间： " + smsResponseTimeout  + "秒"  +
                ", SMS 消息重传次数： " + smsRetransmissionCount +
                ", 主服务器APN无线通信拨号访问点： " + apnMaster + '\'' +
                ", 主服务器无线通信拨号用户名： " + dialingUsernameMaster  +
                ", 主服务器无线通信拨号密码： " + dialingPasswordMaster  +
                ", 主服务器地址IP或域名： " + addressMaster  +
                ", 备份服务器APN： " + apnBackup  +
                ", 备份服务器无线通信拨号用户名： " + dialingUsernameBackup  +
                ", 备份服务器无线通信拨号密码： " + dialingPasswordBackup  +
                ", 备用服务器备份地址IP或域名： " + addressBackup  +
                ", 道路运输证IC卡认证主服务器IP地址或域名： " + addressIcMaster  +
                ", 道路运输证IC卡认证主服务器TCP端口： " + tcpPortIcMaster +
                ", 道路运输证IC卡认证主服务器UDP端口： " + udpPortIcMaster +
                ", 道路运输证IC卡认证备份服务器IP地址或域名： " + addressIcBackup  +
                ", 位置汇报策略： " + locationReportingStrategy +
                ", 位置汇报方案： " + locationReportingPlan +
                ", 驾驶员未登录汇报时间间隔： " + reportingIntervalOffline + "秒"   +
                ", 从服务器 APN： " + apnSlave  +
                ", 从服务器无线通信拨号密码： " + dialingUsernameSlave  +
                ", 从服务器备份地址 IP或域名： " + dialingPasswordSlave  +
                ", 从服务器备份地址 IP或域名： " + addressSlave  +
                ", reportingIntervalDormancy： " + reportingIntervalDormancy +
                ", reportingIntervalEmergencyAlarm： " + reportingIntervalEmergencyAlarm +
                ", reportingIntervalDefault： " + reportingIntervalDefault +
                ", reportingDistanceDefault： " + reportingDistanceDefault +
                ", reportingDistanceOffline： " + reportingDistanceOffline +
                ", reportingDistanceDormancy： " + reportingDistanceDormancy +
                ", reportingDistanceEmergencyAlarm： " + reportingDistanceEmergencyAlarm +
                ", inflectionPointAngle： " + inflectionPointAngle +
                ", fenceRadius： " + fenceRadius +
                ", illegalDrivingPeriods： " + illegalDrivingPeriods +
                ", platformPhoneNumber： " + platformPhoneNumber  +
                ", phoneNumberForReset： " + phoneNumberForReset  +
                ", phoneNumberForFactoryReset： " + phoneNumberForFactoryReset  +
                ", phoneNumberForSms： " + phoneNumberForSms  +
                ", phoneNumberForReceiveTextAlarm： " + phoneNumberForReceiveTextAlarm  +
                ", phoneAnsweringPolicy： " + phoneAnsweringPolicy +
                ", longestCallTimeForPerSession： " + longestCallTimeForPerSession +
                ", longestCallTimeInMonth： " + longestCallTimeInMonth +
                ", phoneNumbersForListen： " + phoneNumbersForListen  +
                ", privilegedSMSNumber： " + privilegedSMSNumber  +
                ", alarmMaskingWord： " + alarmMaskingWord +
                ", alarmSendsTextSmsSwitch： " + alarmSendsTextSmsSwitch +
                ", alarmShootingSwitch： " + alarmShootingSwitch +
                ", alarmShootingStorageFlags： " + alarmShootingStorageFlags +
                ", KeySign： " + KeySign +
                ", topSpeed： " + maxSpeed +
                ", overSpeedDuration： " + overSpeedDuration +
                ", continuousDrivingTimeThreshold： " + continuousDrivingTimeThreshold +
                ", cumulativeDrivingTimeThresholdForTheDay： " + cumulativeDrivingTimeThresholdForTheDay +
                ", minimumBreakTime： " + minimumBreakTime +
                ", maximumParkingTime： " + maximumParkingTime +
                ", overSpeedWarningDifference： " + overSpeedWarningDifference +
                ", drowsyDrivingWarningDifference： " + drowsyDrivingWarningDifference +
                ", collisionAlarmParams： " + collisionAlarmParams +
                ", rolloverAlarm： " + rolloverAlarm +
                ", cameraTimer： " + cameraTimer +
                ", qualityForVideo： " + qualityForVideo +
                ", brightness： " + brightness +
                ", contrastRatio： " + contrastRatio +
                ", saturation： " + saturation +
                ", chroma： " + chroma +
                ", mileage： " + mileage +
                ", provincialId： " + provincialId +
                ", cityId： " + cityId +
                ", licensePlate： " + licensePlate  +
                ", licensePlateColor： " + licensePlateColor +
                ", gnssPositioningMode： " + gnssPositioningMode +
                ", gnssBaudRate： " + gnssBaudRate +
                ", gnssOutputFrequency： " + gnssOutputFrequency +
                ", gnssCollectionFrequency： " + gnssCollectionFrequency +
                ", gnssDataUploadMethod： " + gnssDataUploadMethod +
                ", gnssDataUploadMethodUnit： " + gnssDataUploadMethodUnit +
                ", canCollectionTimeForChannel1： " + canCollectionTimeForChannel1 +
                ", canUploadIntervalForChannel1： " + canUploadIntervalForChannel1 +
                ", canCollectionTimeForChannel2： " + canCollectionTimeForChannel2 +
                ", canUploadIntervalForChannel2： " + canUploadIntervalForChannel2 +
                '}';
    }
}
