package com.genersoft.iot.vmp.gb28181.bean;

import java.util.Map;


public class Device {

	/**
	 * 设备Id
	 */
	private String deviceId;

	/**
	 * 设备名
	 */
	private String name;
	
	/**
	 * 生产厂商
	 */
	private String manufacturer;
	
	/**
	 * 型号
	 */
	private String model;
	
	/**
	 * 固件版本
	 */
	private String firmware;

	/**
	 * 传输协议
	 * UDP/TCP
	 */
	private String transport;

	/**
	 * wan地址
	 */
	private Host host;
	
	/**
	 * 在线
	 */
	private int online;

	/**
	 * 通道列表
	 */
	private Map<String,DeviceChannel> channelMap;


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

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public Map<String, DeviceChannel> getChannelMap() {
		return channelMap;
	}

	public void setChannelMap(Map<String, DeviceChannel> channelMap) {
		this.channelMap = channelMap;
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

	public int getOnline() {
		return online;
	}

	public void setOnline(int online) {
		this.online = online;
	}
}
