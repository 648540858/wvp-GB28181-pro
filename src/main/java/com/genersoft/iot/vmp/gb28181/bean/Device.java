package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
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
//	private Map<String,DeviceChannel> channelMap;

	private int channelCount;

	private List<String> channelList;

}
