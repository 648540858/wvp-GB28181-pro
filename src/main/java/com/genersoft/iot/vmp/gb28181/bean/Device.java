package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 国标设备/平台
 * @author lin
 */
@Schema(description = "国标设备/平台")
public class Device {

	/**
	 * 设备国标编号
	 */
	@Schema(description = "设备国标编号")
	private String deviceId;

	/**
	 * 设备名
	 */
	@Schema(description = "名称")
	private String name;
	
	/**
	 * 生产厂商
	 */
	@Schema(description = "生产厂商")
	private String manufacturer;
	
	/**
	 * 型号
	 */
	@Schema(description = "型号")
	private String model;
	
	/**
	 * 固件版本
	 */
	@Schema(description = "固件版本")
	private String firmware;

	/**
	 * 传输协议
	 * UDP/TCP
	 */
	@Schema(description = "传输协议（UDP/TCP）")
	private String transport;

	/**
	 * 数据流传输模式
	 * UDP:udp传输
	 * TCP-ACTIVE：tcp主动模式
	 * TCP-PASSIVE：tcp被动模式
	 */
	@Schema(description = "数据流传输模式")
	private String streamMode;

	/**
	 * wan地址_ip
	 */
	@Schema(description = "IP")
	private String  ip;

	/**
	 * wan地址_port
	 */
	@Schema(description = "端口")
	private int port;

	/**
	 * wan地址
	 */
	@Schema(description = "wan地址")
	private String  hostAddress;
	
	/**
	 * 在线
	 */
	@Schema(description = "是否在线，true为在线，false为离线")
	private boolean onLine;


	/**
	 * 注册时间
	 */
	@Schema(description = "注册时间")
	private String registerTime;


	/**
	 * 心跳时间
	 */
	@Schema(description = "心跳时间")
	private String keepaliveTime;


	/**
	 * 心跳间隔
	 */
	@Schema(description = "心跳间隔")
	private int keepaliveIntervalTime;

	/**
	 * 通道个数
	 */
	@Schema(description = "通道个数")
	private int channelCount;

	/**
	 * 注册有效期
	 */
	@Schema(description = "注册有效期")
	private int expires;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private String createTime;

	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private String updateTime;

	/**
	 * 设备使用的媒体id, 默认为null
	 */
	@Schema(description = "设备使用的媒体id, 默认为null")
	private String mediaServerId;

	/**
	 * 字符集, 支持 UTF-8 与 GB2312
	 */
	@Schema(description = "符集, 支持 UTF-8 与 GB2312")
	private String charset ;

	/**
	 * 目录订阅周期，0为不订阅
	 */
	@Schema(description = "目录订阅周期，o为不订阅")
	private int subscribeCycleForCatalog;

	/**
	 * 移动设备位置订阅周期，0为不订阅
	 */
	@Schema(description = "移动设备位置订阅周期，0为不订阅")
	private int subscribeCycleForMobilePosition;

	/**
	 * 移动设备位置信息上报时间间隔,单位:秒,默认值5
	 */
	@Schema(description = "移动设备位置信息上报时间间隔,单位:秒,默认值5")
	private int mobilePositionSubmissionInterval = 5;

	/**
	 * 报警订阅周期，0为不订阅
	 */
	@Schema(description = "报警心跳时间订阅周期，0为不订阅")
	private int subscribeCycleForAlarm;

	/**
	 * 是否开启ssrc校验，默认关闭，开启可以防止串流
	 */
	@Schema(description = "是否开启ssrc校验，默认关闭，开启可以防止串流")
	private boolean ssrcCheck = false;

	/**
	 * 地理坐标系， 目前支持 WGS84,GCJ02
	 */
	@Schema(description = "地理坐标系， 目前支持 WGS84,GCJ02")
	private String geoCoordSys;

	@Schema(description = "密码")
	private String password;

	@Schema(description = "收流IP")
	private String sdpIp;

	@Schema(description = "SIP交互IP（设备访问平台的IP）")
	private String localIp;

	@Schema(description = "是否作为消息通道")
	private boolean asMessageChannel;

	@Schema(description = "设备注册的事务信息")
	private SipTransactionInfo sipTransactionInfo;




	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getFirmware() {
		return firmware;
	}

	public void setFirmware(String firmware) {
		this.firmware = firmware;
	}

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public String getStreamMode() {
		return streamMode;
	}

	public Integer getStreamModeForParam() {
		if (streamMode == null) {
			return 0;
		}
		if (streamMode.equalsIgnoreCase("UDP")) {
			return 0;
		}else if (streamMode.equalsIgnoreCase("TCP-PASSIVE")) {
			return 1;
		}else if (streamMode.equalsIgnoreCase("TCP-ACTIVE")) {
			return 2;
		}
		return 0;
	}

	public void setStreamMode(String streamMode) {
		this.streamMode = streamMode;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public boolean isOnLine() {
		return onLine;
	}

	public void setOnLine(boolean onLine) {
		this.onLine = onLine;
	}

	public int getChannelCount() {
		return channelCount;
	}

	public void setChannelCount(int channelCount) {
		this.channelCount = channelCount;
	}

	public String getRegisterTime() {
		return registerTime;
	}

	public void setRegisterTime(String registerTime) {
		this.registerTime = registerTime;
	}

	public String getKeepaliveTime() {
		return keepaliveTime;
	}

	public void setKeepaliveTime(String keepaliveTime) {
		this.keepaliveTime = keepaliveTime;
	}

	public int getExpires() {
		return expires;
	}

	public void setExpires(int expires) {
		this.expires = expires;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public String getMediaServerId() {
		return mediaServerId;
	}

	public void setMediaServerId(String mediaServerId) {
		this.mediaServerId = mediaServerId;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public int getSubscribeCycleForCatalog() {
		return subscribeCycleForCatalog;
	}

	public void setSubscribeCycleForCatalog(int subscribeCycleForCatalog) {
		this.subscribeCycleForCatalog = subscribeCycleForCatalog;
	}

	public int getSubscribeCycleForMobilePosition() {
		return subscribeCycleForMobilePosition;
	}

	public void setSubscribeCycleForMobilePosition(int subscribeCycleForMobilePosition) {
		this.subscribeCycleForMobilePosition = subscribeCycleForMobilePosition;
	}

	public int getMobilePositionSubmissionInterval() {
		return mobilePositionSubmissionInterval;
	}

	public void setMobilePositionSubmissionInterval(int mobilePositionSubmissionInterval) {
		this.mobilePositionSubmissionInterval = mobilePositionSubmissionInterval;
	}

	public int getSubscribeCycleForAlarm() {
		return subscribeCycleForAlarm;
	}

	public void setSubscribeCycleForAlarm(int subscribeCycleForAlarm) {
		this.subscribeCycleForAlarm = subscribeCycleForAlarm;
	}

	public boolean isSsrcCheck() {
		return ssrcCheck;
	}

	public void setSsrcCheck(boolean ssrcCheck) {
		this.ssrcCheck = ssrcCheck;
	}

	public String getGeoCoordSys() {
		return geoCoordSys;
	}

	public void setGeoCoordSys(String geoCoordSys) {
		this.geoCoordSys = geoCoordSys;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSdpIp() {
		return sdpIp;
	}

	public void setSdpIp(String sdpIp) {
		this.sdpIp = sdpIp;
	}

	public String getLocalIp() {
		return localIp;
	}

	public void setLocalIp(String localIp) {
		this.localIp = localIp;
	}

	public int getKeepaliveIntervalTime() {
		return keepaliveIntervalTime;
	}

	public void setKeepaliveIntervalTime(int keepaliveIntervalTime) {
		this.keepaliveIntervalTime = keepaliveIntervalTime;
	}

	public boolean isAsMessageChannel() {
		return asMessageChannel;
	}

	public void setAsMessageChannel(boolean asMessageChannel) {
		this.asMessageChannel = asMessageChannel;
	}

	public SipTransactionInfo getSipTransactionInfo() {
		return sipTransactionInfo;
	}

	public void setSipTransactionInfo(SipTransactionInfo sipTransactionInfo) {
		this.sipTransactionInfo = sipTransactionInfo;
	}

	/*======================设备主子码流逻辑START=========================*/
	@Schema(description = "开启主子码流切换的开关（false-不开启，true-开启）")
	private boolean switchPrimarySubStream;

	public boolean isSwitchPrimarySubStream() {
		return switchPrimarySubStream;
	}

	public void setSwitchPrimarySubStream(boolean switchPrimarySubStream) {
		this.switchPrimarySubStream = switchPrimarySubStream;
	}

	/*======================设备主子码流逻辑END=========================*/


}
