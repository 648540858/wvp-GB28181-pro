package com.genersoft.iot.vmp.gb28181.bean;


public class DeviceAlarm {

	/**
	 * 数据库id
	 */
	private String id;

	/**
	 * 设备Id
	 */
	private String deviceId;

	/**
	 * 通道Id
	 */
	private String channelId;

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
	 * 报警类型,
	 * 报警方式为2时,不携带 AlarmType为默认的报警设备报警,
	 * 携带 AlarmType取值及对应报警类型如下:
	 * 		1-视频丢失报警;
	 * 		2-设备防拆报警;
	 * 		3-存储设备磁盘满报警;
	 * 		4-设备高温报警;
	 * 		5-设备低温报警。
	 * 报警方式为5时,取值如下:
	 * 		1-人工视频报警;
	 * 		2-运动目标检测报警;
	 * 		3-遗留物检测报警;
	 * 		4-物体移除检测报警;
	 * 		5-绊线检测报警;
	 * 		6-入侵检测报警;
	 * 		7-逆行检测报警;
	 * 		8-徘徊检测报警;
	 * 		9-流量统计报警;
	 * 		10-密度检测报警;
	 * 		11-视频异常检测报警;
	 * 		12-快速移动报警。
	 * 报警方式为6时,取值下:
	 * 		1-存储设备磁盘故障报警;
	 * 		2-存储设备风扇故障报警。
	 */
	private String alarmType;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
}
