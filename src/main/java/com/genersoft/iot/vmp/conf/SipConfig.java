package com.genersoft.iot.vmp.conf;


import org.springframework.core.annotation.Order;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sip", ignoreInvalidFields = true)
@Order(0)
public class SipConfig {

	private String ip;

	private String showIp;

	private Integer port;

	private String domain;

	private String id;

	private String password;
	
	Integer ptzSpeed = 50;

	Integer registerTimeInterval = 120;

	private boolean alarm;

	private String name = "WVP视频平台";

	/**
	 * 平台厂商
	 */
	private String manufacturer = "WVP视频平台";

	/**
	 * 平台型号
	 */
	private String model;

	/**
	 * 平台归属
	 */
	private String owner;

	/**
	 * 平台行政区划
	 */
	private String civilCode;

	/**
	 * 平台安装地址
	 */
	private String address;

	/**
	 * 注册方式
	 */
	private int registerWay = 1;

	/**
	 * 是否保密
	 */
	private int secrecy = 0;

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPtzSpeed(Integer ptzSpeed) {
		this.ptzSpeed = ptzSpeed;
	}


	public void setRegisterTimeInterval(Integer registerTimeInterval) {
		this.registerTimeInterval = registerTimeInterval;
	}

	public String getIp() {
		return ip;
	}


	public Integer getPort() {
		return port;
	}


	public String getDomain() {
		return domain;
	}


	public String getId() {
		return id;
	}

	public String getPassword() {
		return password;
	}


	public Integer getPtzSpeed() {
		return ptzSpeed;
	}

	public Integer getRegisterTimeInterval() {
		return registerTimeInterval;
	}

	public boolean isAlarm() {
		return alarm;
	}

	public void setAlarm(boolean alarm) {
		this.alarm = alarm;
	}

	public String getShowIp() {
		if (this.showIp == null) {
			return this.ip;
		}
		return showIp;
	}

	public void setShowIp(String showIp) {
		this.showIp = showIp;
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

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getRegisterWay() {
		return registerWay;
	}

	public void setRegisterWay(int registerWay) {
		this.registerWay = registerWay;
	}

	public int getSecrecy() {
		return secrecy;
	}

	public void setSecrecy(int secrecy) {
		this.secrecy = secrecy;
	}
}
