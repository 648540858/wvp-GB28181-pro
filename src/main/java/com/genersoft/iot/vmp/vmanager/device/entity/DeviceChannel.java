package com.genersoft.iot.vmp.vmanager.device.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description:设备通道信息
 * @author: songww
 * @date:   2020年5月20日 下午9:00:46     
 */
@ApiModel(value = "设备通道信息", description = "设备通道信息")
@Table(name="VMP_VIDEOCHANNELS")
public class DeviceChannel {

	/**
	 * 通道编号
	 */
	@ApiModelProperty("通道编号")
	@Id
	@Column(name="CHANNEL_ID")
	private String channelId;
	
	/**
	 * 设备编号
	 */
	@ApiModelProperty("设备编号")
	@Column(name="DEVICE_ID")
	private String deviceId;
	
	/**
	 * 通道名
	 */
	@ApiModelProperty("通道名")
	@Column(name="CHANNEL_NAME")
	private String channelName;
	
	/**
	 * 生产厂商
	 */
	@ApiModelProperty("生产厂商")
	@Column(name="MANUFACTURER")
	private String manufacture;
	
	/**
	 * 型号
	 */
	@ApiModelProperty("型号")
	@Column(name="MODEL")
	private String model;
	
	/**
	 * 设备归属
	 */
	@ApiModelProperty("设备归属")
	@Column(name="OWNER")
	private String owner;
	
	/**
	 * 行政区域
	 */
	@ApiModelProperty("行政区域")
	@Column(name="CIVIL_CODE")
	private String civilCode;
	
	/**
	 * 警区
	 */
	@ApiModelProperty("警区")
	@Column(name="BLOCK")
	private String block;

	/**
	 * 安装地址
	 */
	@ApiModelProperty("安装地址")
	@Column(name="ADDRESS")
	private String address;
	
	/**
	 * 是否有子设备 1有, 0没有
	 */
	@ApiModelProperty("是否有子设备")
	@Column(name="PARENTAL")
	private String parental;
	
	/**
	 * 父级id
	 */
	@ApiModelProperty("父级编码")
	@Column(name="PARENT_ID")
	private String parentId;
	
	/**
	 * 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式
	 */
	@ApiModelProperty("信令安全模式")
	@Column(name="SAFETY_WAY")
	private String safetyWay;
	
	/**
	 * 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式
	 */
	@ApiModelProperty("注册方式")
	@Column(name="REGISTER_WAY")
	private String registerWay;
	
	/**
	 * 证书序列号
	 */
	@ApiModelProperty("证书序列号")
	@Column(name="CERT_NUM")
	private String certNum;
	
	/**
	 * 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效
	 */
	@ApiModelProperty("证书有效标识")
	@Column(name="CERT_VALID")
	private String certValid;
	
	/**
	 * 证书无效原因码
	 */
	@ApiModelProperty("证书无效原因码")
	@Column(name="CERT_ERRCODE")
	private String certErrCode;
	
	/**
	 * 证书终止有效期
	 */
	@ApiModelProperty("证书终止有效期")
	@Column(name="CERT_ENDTIME")
	private String certEndTime;
	
	/**
	 * 保密属性 缺省为0; 0:不涉密, 1:涉密
	 */
	@ApiModelProperty("保密属性")
	@Column(name="SECRECY")
	private String secrecy;
	
	/**
	 * IP地址
	 */
	@ApiModelProperty("IP地址")
	@Column(name="IP")
	private String ip;
	
	/**
	 * 端口号
	 */
	@ApiModelProperty("端口号")
	@Column(name="PORT")
	private Integer port;
	
	/**
	 * 密码
	 */
	@ApiModelProperty("密码")
	@Column(name="PASSWORD")
	private String password;	 
	
	/**
	 * 在线/离线
	 * 1在线,0离线
	 * 默认在线
	 * 信令:
	 * <Status>ON</Status>
	 * <Status>OFF</Status>
	 * 遇到过NVR下的IPC下发信令可以推流， 但是 Status 响应 OFF
	 */
	@ApiModelProperty("状态")
	@Column(name="ONLINE")
	private String online;

	/**
	 * 经度
	 */
	@ApiModelProperty("经度")
	@Column(name="LONGITUDE")
	private double longitude;
	
	/**
	 * 纬度
	 */
	@ApiModelProperty("纬度")
	@Column(name="LATITUDE")
	private double latitude;

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
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

	public String getParental() {
		return parental;
	}

	public void setParental(String parental) {
		this.parental = parental;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getSafetyWay() {
		return safetyWay;
	}

	public void setSafetyWay(String safetyWay) {
		this.safetyWay = safetyWay;
	}

	public String getRegisterWay() {
		return registerWay;
	}

	public void setRegisterWay(String registerWay) {
		this.registerWay = registerWay;
	}

	public String getCertNum() {
		return certNum;
	}

	public void setCertNum(String certNum) {
		this.certNum = certNum;
	}

	public String getCertValid() {
		return certValid;
	}

	public void setCertValid(String certValid) {
		this.certValid = certValid;
	}

	public String getCertErrCode() {
		return certErrCode;
	}

	public void setCertErrCode(String certErrCode) {
		this.certErrCode = certErrCode;
	}

	public String getCertEndTime() {
		return certEndTime;
	}

	public void setCertEndTime(String certEndTime) {
		this.certEndTime = certEndTime;
	}

	public String getSecrecy() {
		return secrecy;
	}

	public void setSecrecy(String secrecy) {
		this.secrecy = secrecy;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOnline() {
		return online;
	}

	public void setOnline(String online) {
		this.online = online;
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
}
