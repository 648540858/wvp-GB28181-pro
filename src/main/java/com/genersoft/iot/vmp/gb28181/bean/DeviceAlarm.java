package com.genersoft.iot.vmp.gb28181.bean;


public class DeviceAlarm {

	/**
	 * 设备Id
	 */
	private String deviceId;

	/**
	 * 报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级 警情-
	 */
	private String alarmPriority;

	/**
	 * 报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,
	 * 7其他报警;可以为直接组合如12为电话报警或 设备报警-
	 */
	private String alarmMethod;

	/**
	 * 报警时间
	 */
	private String alarmTime;

	/**
	 * 报警内容描述
	 */
	private String alarmDescription;

	/**
	 * 经度
	 */
	private double longitude;

	/**
	 * 纬度
	 */
	private double latitude;

	/**
	 * 报警类型
	 */
	private String alarmType;


	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getAlarmPriority() {
		return alarmPriority;
	}

	public void setAlarmPriority(String alarmPriority) {
		this.alarmPriority = alarmPriority;
	}

	public String getAlarmMethod() {
		return alarmMethod;
	}

	public void setAlarmMethod(String alarmMethod) {
		this.alarmMethod = alarmMethod;
	}

	public String getAlarmTime() {
		return alarmTime;
	}

	public void setAlarmTime(String alarmTime) {
		this.alarmTime = alarmTime;
	}

	public String getAlarmDescription() {
		return alarmDescription;
	}

	public void setAlarmDescription(String alarmDescription) {
		this.alarmDescription = alarmDescription;
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

	public String getAlarmType() {
		return alarmType;
	}

	public void setAlarmType(String alarmType) {
		this.alarmType = alarmType;
	}
}
