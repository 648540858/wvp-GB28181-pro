package com.genersoft.iot.vmp.vmanager.device.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description:视频设备信息
 * @author: songww
 * @date:   2020年5月8日 下午2:05:56
 */
@Data
@ApiModel(value = "视频设备信息", description = "视频设备信息")
@Table(name="VMP_VIDEODEVICES")
public class Device {

	/**
	 * 设备Id
	 */
	@ApiModelProperty("设备编号")
	@Id
	@Column(name="DEVICE_ID")
	@NotNull(message = "deviceId 不能为 null")
	@Size(min = 4, max = 32, message = "deviceId 必须大于 4 位并且小于 32 位")
	private String deviceId;

	/**
	 * 设备名称
	 */
	@ApiModelProperty("设备名称")
	@Column(name="DEVICE_NAME")
	@Size(max = 32, message = "deviceName 必须小于 32 位")
	private String deviceName;

	/**
	 * 生产厂商
	 */
	@ApiModelProperty("生产厂商")
	@Column(name="MANUFACTURER")
	@Size(max = 64, message = "manufacturer 必须小于 64 位")
	private String manufacturer;

	/**
	 * 型号
	 */
	@ApiModelProperty("型号")
	@Column(name="MODEL")
	@Size(max = 64, message = "manufacturer 必须小于 64 位")
	private String model;

	/**
	 * 固件版本
	 */
	@ApiModelProperty("固件版本")
	@Column(name="FIRMWARE")
	@Size(max = 64, message = "firmware 必须小于 64 位")
	private String firmware;

	/**
	 * 通信协议
	 * GB28181 ONVIF
	 */
	@ApiModelProperty("通信协议")
	@Column(name="PROTOCOL")
	@NotNull(message = "protocol 不能为 null")
	@Size(max = 16, message = "protocol 必须小于 16 位")
	private String protocol;

	/**
	 * SIP 传输协议
	 * UDP/TCP
	 */
	@ApiModelProperty("SIP 传输协议")
	@Column(name="TRANSPORT")
	@Size(min = 3,max = 3 ,message = "transport 必须为 3 位")
	private String transport;

	/**
	 * 数据流传输模式
	 * UDP:udp传输
	 * TCP-ACTIVE：tcp主动模式
	 * TCP-PASSIVE：tcp被动模式
	 */
	@ApiModelProperty("数据流传输模式")
	@Column(name="STREAM_MODE")
	@Size(max = 64, message = "streamMode 必须小于 16 位")
	private String streamMode;

	/**
	 * IP地址
	 */
	@ApiModelProperty("IP地址")
	@Column(name="IP")
	@Size(max = 15, message = "streamMode 必须小于 15 位")
	private String ip;

	/**
	 * 端口号
	 */
	@ApiModelProperty("端口号")
	@Column(name="PORT")
	@Max(value = 65535,message = "port 最大值为 65535")
	private Integer port;

	/**
	 * 在线状态 1在线, 0离线
	 */
	@ApiModelProperty("在线状态")
	@Size(min = 1,max = 1 ,message = "online 必须为 1 位")
	@Column(name="ONLINE")
	private String online;

	/**
	 * 通道数量
	 */
	@ApiModelProperty("通道数量")
	@Column(name="CHANNEL_SUM")
	@Max(value = 1000000000,message = "channelSum 最大值为 1000000000")
	private Integer channelSum;

	@Override
	public String toString() {
		return "Device{" +
				"deviceId='" + deviceId + '\'' +
				", deviceName='" + deviceName + '\'' +
				", manufacturer='" + manufacturer + '\'' +
				", model='" + model + '\'' +
				", firmware='" + firmware + '\'' +
				", protocol='" + protocol + '\'' +
				", transport='" + transport + '\'' +
				", streamMode='" + streamMode + '\'' +
				", ip='" + ip + '\'' +
				", port=" + port +
				", online='" + online + '\'' +
				", channelSum=" + channelSum +
				", createTime='" + createTime + '\'' +
				", registerTime='" + registerTime + '\'' +
				", heartbeatTime='" + heartbeatTime + '\'' +
				", updateTime='" + updateTime + '\'' +
				", updatePerson='" + updatePerson + '\'' +
				", syncTime='" + syncTime + '\'' +
				", syncPerson='" + syncPerson + '\'' +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				", channelList=" + channelList +
				'}';
	}

	/**
	 * 创建时间
	 */
	@ApiModelProperty("创建时间")
	@Column(name="CREATE_TIME")
	private String createTime;

	/**
	 * 注册时间
	 */
	@ApiModelProperty("注册时间")
	@Column(name="REGISTER_TIME")
	private String registerTime;

	/**
	 * 心跳时间
	 */
	@ApiModelProperty("心跳时间")
	@Column(name="HEARTBEAT_TIME")
	private String heartbeatTime;

	/**
	 * 修改时间
	 */
	@ApiModelProperty("更新时间")
	@Column(name="UPDATE_TIME")
	private String updateTime;

	/**
	 * 修改人
	 */
	@ApiModelProperty("修改人")
	@Column(name="UPDATE_PERSON")
	private String updatePerson;

	/**
	 * 同步时间
	 */
	@ApiModelProperty("同步时间")
	@Column(name="SYNC_TIME")
	private String syncTime;

	/**
	 * 同步人
	 */
	@ApiModelProperty("同步人")
	@Column(name="SYNC_PERSON")
	private String syncPerson;

	/**
	 * ONVIF协议-用户名
	 */
	@ApiModelProperty("用户名")
	@Column(name="USERNAME")
	@Size(max = 32, message = "username 必须小于 32 位")
	private String username;

	/**
	 * ONVIF协议-密码
	 */
	@ApiModelProperty("密码")
	@Size(max = 32, message = "password 必须小于 32 位")
	@Column(name="PASSWORD")
	private String password;

	@Transient
	private List<DeviceChannel> channelList;

}
