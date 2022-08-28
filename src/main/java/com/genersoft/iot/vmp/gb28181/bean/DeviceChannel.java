package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通道信息")
public class DeviceChannel {


	/**
	 * 数据库自增ID
	 */
	@Schema(description = "数据库自增ID")
	private int id;

	/**
	 * 通道国标编号
	 */
	@Schema(description = "通道国标编号")
	private String channelId;

	/**
	 * 设备国标编号
	 */
	@Schema(description = "设备国标编号")
	private String deviceId;
	
	/**
	 * 通道名
	 */
	@Schema(description = "名称")
	private String name;
	
	/**
	 * 生产厂商
	 */
	@Schema(description = "生产厂商")
	private String manufacture;
	
	/**
	 * 型号
	 */
	@Schema(description = "型号")
	private String model;
	
	/**
	 * 设备归属
	 */
	@Schema(description = "设备归属")
	private String owner;
	
	/**
	 * 行政区域
	 */
	@Schema(description = "行政区域")
	private String civilCode;
	
	/**
	 * 警区
	 */
	@Schema(description = "警区")
	private String block;

	/**
	 * 安装地址
	 */
	@Schema(description = "安装地址")
	private String address;
	
	/**
	 * 是否有子设备 1有, 0没有
	 */
	@Schema(description = "是否有子设备 1有, 0没有")
	private int parental;
	
	/**
	 * 父级id
	 */
	@Schema(description = "父级id")
	private String parentId;
	
	/**
	 * 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式
	 */
	@Schema(description = "信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式")
	private int safetyWay;
	
	/**
	 * 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式
	 */
	@Schema(description = "注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式")
	private int registerWay;
	
	/**
	 * 证书序列号
	 */
	@Schema(description = "证书序列号")
	private String certNum;
	
	/**
	 * 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效
	 */
	@Schema(description = "证书有效标识 缺省为0;证书有效标识:0:无效1: 有效")
	private int certifiable;
	
	/**
	 * 证书无效原因码
	 */
	@Schema(description = "证书无效原因码")
	private int errCode;
	
	/**
	 * 证书终止有效期
	 */
	@Schema(description = "证书终止有效期")
	private String endTime;
	
	/**
	 * 保密属性 缺省为0; 0:不涉密, 1:涉密
	 */
	@Schema(description = "保密属性 缺省为0; 0:不涉密, 1:涉密")
	private String secrecy;
	
	/**
	 * IP地址
	 */
	@Schema(description = "IP地址")
	private String ipAddress;
	
	/**
	 * 端口号
	 */
	@Schema(description = "端口号")
	private int port;
	
	/**
	 * 密码
	 */
	@Schema(description = "密码")
	private String password;

	/**
	 * 云台类型
	 */
	@Schema(description = "云台类型")
	private int PTZType;

	/**
	 * 云台类型描述字符串
	 */
	@Schema(description = "云台类型描述字符串")
	private String PTZTypeText;

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
	 * 在线/离线
	 * 1在线,0离线
	 * 默认在线
	 * 信令:
	 * <Status>ON</Status>
	 * <Status>OFF</Status>
	 * 遇到过NVR下的IPC下发信令可以推流， 但是 Status 响应 OFF
	 */
	@Schema(description = "在线/离线， 1在线,0离线")
	private int status;

	/**
	 * 经度
	 */
	@Schema(description = "经度")
	private double longitude;
	
	/**
	 * 纬度
	 */
	@Schema(description = "纬度")
	private double latitude;

	/**
	 * 经度 GCJ02
	 */
	@Schema(description = "GCJ02坐标系经度")
	private double longitudeGcj02;

	/**
	 * 纬度 GCJ02
	 */
	@Schema(description = "GCJ02坐标系纬度")
	private double latitudeGcj02;

	/**
	 * 经度 WGS84
	 */
	@Schema(description = "WGS84坐标系经度")
	private double longitudeWgs84;

	/**
	 * 纬度 WGS84
	 */
	@Schema(description = "WGS84坐标系纬度")
	private double latitudeWgs84;

	/**
	 * 子设备数
	 */
	@Schema(description = "子设备数")
	private int subCount;

	/**
	 * 流唯一编号，存在表示正在直播
	 */
	@Schema(description = "流唯一编号，存在表示正在直播")
	private String  streamId;

	/**
	 *  是否含有音频
	 */
	@Schema(description = "是否含有音频")
	private boolean hasAudio;

	/**
	 * 标记通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划
	 */
	@Schema(description = "标记通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划")
	private int channelType;

	/**
	 * 业务分组
	 */
	@Schema(description = "业务分组")
	private String businessGroupId;

	/**
	 * GPS的更新时间
	 */
	@Schema(description = "GPS的更新时间")
	private String gpsTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public void setPTZType(int PTZType) {
		this.PTZType = PTZType;
		switch (PTZType) {
			case 0:
				this.PTZTypeText = "未知";
				break;
			case 1:
				this.PTZTypeText = "球机";
				break;
			case 2:
				this.PTZTypeText = "半球";
				break;
			case 3:
				this.PTZTypeText = "固定枪机";
				break;
			case 4:
				this.PTZTypeText = "遥控枪机";
				break;
		}
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getManufacture() {
		return manufacture;
	}

	public void setManufacture(String manufacture) {
		this.manufacture = manufacture;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getCivilCode() {
		return civilCode;
	}

	public void setCivilCode(String civilCode) {
		this.civilCode = civilCode;
	}

	public String getBlock() {
		return block;
	}

	public void setBlock(String block) {
		this.block = block;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getParental() {
		return parental;
	}

	public void setParental(int parental) {
		this.parental = parental;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public int getSafetyWay() {
		return safetyWay;
	}

	public void setSafetyWay(int safetyWay) {
		this.safetyWay = safetyWay;
	}

	public int getRegisterWay() {
		return registerWay;
	}

	public void setRegisterWay(int registerWay) {
		this.registerWay = registerWay;
	}

	public String getCertNum() {
		return certNum;
	}

	public void setCertNum(String certNum) {
		this.certNum = certNum;
	}

	public int getCertifiable() {
		return certifiable;
	}

	public void setCertifiable(int certifiable) {
		this.certifiable = certifiable;
	}

	public int getErrCode() {
		return errCode;
	}

	public void setErrCode(int errCode) {
		this.errCode = errCode;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getSecrecy() {
		return secrecy;
	}

	public void setSecrecy(String secrecy) {
		this.secrecy = secrecy;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPTZType() {
		return PTZType;
	}

	public String getPTZTypeText() {
		return PTZTypeText;
	}

	public void setPTZTypeText(String PTZTypeText) {
		this.PTZTypeText = PTZTypeText;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitudeGcj02() {
		return longitudeGcj02;
	}

	public void setLongitudeGcj02(double longitudeGcj02) {
		this.longitudeGcj02 = longitudeGcj02;
	}

	public double getLatitudeGcj02() {
		return latitudeGcj02;
	}

	public void setLatitudeGcj02(double latitudeGcj02) {
		this.latitudeGcj02 = latitudeGcj02;
	}

	public double getLongitudeWgs84() {
		return longitudeWgs84;
	}

	public void setLongitudeWgs84(double longitudeWgs84) {
		this.longitudeWgs84 = longitudeWgs84;
	}

	public double getLatitudeWgs84() {
		return latitudeWgs84;
	}

	public void setLatitudeWgs84(double latitudeWgs84) {
		this.latitudeWgs84 = latitudeWgs84;
	}

	public int getSubCount() {
		return subCount;
	}

	public void setSubCount(int subCount) {
		this.subCount = subCount;
	}

	public boolean isHasAudio() {
		return hasAudio;
	}

	public void setHasAudio(boolean hasAudio) {
		this.hasAudio = hasAudio;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
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

	public int getChannelType() {
		return channelType;
	}

	public void setChannelType(int channelType) {
		this.channelType = channelType;
	}

	public String getBusinessGroupId() {
		return businessGroupId;
	}

	public void setBusinessGroupId(String businessGroupId) {
		this.businessGroupId = businessGroupId;
	}

	public String getGpsTime() {
		return gpsTime;
	}

	public void setGpsTime(String gpsTime) {
		this.gpsTime = gpsTime;
	}
}
