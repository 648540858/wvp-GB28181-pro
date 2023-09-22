package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author lin
 */
@Schema(description = "平台信息")
public class ParentPlatform {

    /**
     * id
     */
    @Schema(description = "ID(数据库中)")
    private Integer id;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private boolean enable;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * SIP服务国标编码
     */
    @Schema(description = "SIP服务国标编码")
    private String serverGBId;

    /**
     * SIP服务国标域
     */
    @Schema(description = "SIP服务国标域")
    private String serverGBDomain;

    /**
     * SIP服务IP
     */
    @Schema(description = "SIP服务IP")
    private String serverIP;

    /**
     * SIP服务端口
     */
    @Schema(description = "SIP服务端口")
    private int serverPort;

    /**
     * 设备国标编号
     */
    @Schema(description = "设备国标编号")
    private String deviceGBId;

    /**
     * 设备ip
     */
    @Schema(description = "设备ip")
    private String deviceIp;

    /**
     * 设备端口
     */
    @Schema(description = "设备端口")
    private String devicePort;

    /**
     * SIP认证用户名(默认使用设备国标编号)
     */
    @Schema(description = "SIP认证用户名(默认使用设备国标编号)")
    private String username;

    /**
     * SIP认证密码
     */
    @Schema(description = "SIP认证密码")
    private String password;

    /**
     * 注册周期 (秒)
     */
    @Schema(description = "注册周期 (秒)")
    private int expires;

    /**
     * 心跳周期(秒)
     */
    @Schema(description = "心跳周期(秒)")
    private int keepTimeout;

    /**
     * 传输协议
     * UDP/TCP
     */
    @Schema(description = "传输协议")
    private String transport;

    /**
     * 字符集
     */
    @Schema(description = "字符集")
    private String characterSet;

    /**
     * 允许云台控制
     */
    @Schema(description = "允许云台控制")
    private boolean ptz;

    /**
     * RTCP流保活
     */
    @Schema(description = "RTCP流保活")
    private boolean rtcp;

    /**
     * 在线状态
     */
    @Schema(description = "在线状态")
    private boolean status;

    /**
     * 在线状态
     */
    @Schema(description = "在线状态")
    private int channelCount;

    /**
     * 默认目录Id,自动添加的通道多放在这个目录下
     */
    @Schema(description = "默认目录Id,自动添加的通道多放在这个目录下")
    private String catalogId;

    /**
     * 已被订阅目录信息
     */
    @Schema(description = "已被订阅目录信息")
    private boolean catalogSubscribe;

    /**
     * 已被订阅报警信息
     */
    @Schema(description = "已被订阅报警信息")
    private boolean alarmSubscribe;

    /**
     * 已被订阅移动位置信息
     */
    @Schema(description = "已被订阅移动位置信息")
    private boolean mobilePositionSubscribe;

    /**
     * 点播未推流的设备时是否使用redis通知拉起
     */
    @Schema(description = "点播未推流的设备时是否使用redis通知拉起")
    private boolean startOfflinePush;

    /**
     * 目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8
     */
    @Schema(description = "目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8")
    private int catalogGroup;

    /**
     * 行政区划
     */
    @Schema(description = "行政区划")
    private String administrativeDivision;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String updateTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "是否作为消息通道")
    private boolean asMessageChannel;

    @Schema(description = "是否作为消息通道")
    private boolean autoPushChannel;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerGBId() {
        return serverGBId;
    }

    public void setServerGBId(String serverGBId) {
        this.serverGBId = serverGBId;
    }

    public String getServerGBDomain() {
        return serverGBDomain;
    }

    public void setServerGBDomain(String serverGBDomain) {
        this.serverGBDomain = serverGBDomain;
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getDeviceGBId() {
        return deviceGBId;
    }

    public void setDeviceGBId(String deviceGBId) {
        this.deviceGBId = deviceGBId;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(String devicePort) {
        this.devicePort = devicePort;
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

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public int getKeepTimeout() {
        return keepTimeout;
    }

    public void setKeepTimeout(int keepTimeout) {
        this.keepTimeout = keepTimeout;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public boolean isPtz() {
        return ptz;
    }

    public void setPtz(boolean ptz) {
        this.ptz = ptz;
    }

    public boolean isRtcp() {
        return rtcp;
    }

    public void setRtcp(boolean rtcp) {
        this.rtcp = rtcp;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public boolean isCatalogSubscribe() {
        return catalogSubscribe;
    }

    public void setCatalogSubscribe(boolean catalogSubscribe) {
        this.catalogSubscribe = catalogSubscribe;
    }

    public boolean isAlarmSubscribe() {
        return alarmSubscribe;
    }

    public void setAlarmSubscribe(boolean alarmSubscribe) {
        this.alarmSubscribe = alarmSubscribe;
    }

    public boolean isMobilePositionSubscribe() {
        return mobilePositionSubscribe;
    }

    public void setMobilePositionSubscribe(boolean mobilePositionSubscribe) {
        this.mobilePositionSubscribe = mobilePositionSubscribe;
    }

    public boolean isStartOfflinePush() {
        return startOfflinePush;
    }

    public void setStartOfflinePush(boolean startOfflinePush) {
        this.startOfflinePush = startOfflinePush;
    }

    public int getCatalogGroup() {
        return catalogGroup;
    }

    public void setCatalogGroup(int catalogGroup) {
        this.catalogGroup = catalogGroup;
    }

    public String getAdministrativeDivision() {
        return administrativeDivision;
    }

    public void setAdministrativeDivision(String administrativeDivision) {
        this.administrativeDivision = administrativeDivision;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public boolean isAsMessageChannel() {
        return asMessageChannel;
    }

    public void setAsMessageChannel(boolean asMessageChannel) {
        this.asMessageChannel = asMessageChannel;
    }

    public boolean isAutoPushChannel() {
        return autoPushChannel;
    }

    public void setAutoPushChannel(boolean autoPushChannel) {
        this.autoPushChannel = autoPushChannel;
    }
}
