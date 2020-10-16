package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class DeviceChannel {

	/**
	 * 通道id
	 */
	private String channelId;
	
	/**
	 * 通道名
	 */
	private String name;
	
	/**
	 * 生产厂商
	 */
	private String manufacture;
	
	/**
	 * 型号
	 */
	private String model;
	
	/**
	 * 设备归属
	 */
	private String owner;
	
	/**
	 * 行政区域
	 */
	private String civilCode;
	
	/**
	 * 警区
	 */
	private String block;

	/**
	 * 安装地址
	 */
	private String address;
	
	/**
	 * 是否有子设备 1有, 0没有
	 */
	private int parental;
	
	/**
	 * 父级id
	 */
	private String parentId;
	
	/**
	 * 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式
	 */
	private int safetyWay;
	
	/**
	 * 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式
	 */
	private int registerWay;
	
	/**
	 * 证书序列号
	 */
	private String certNum;
	
	/**
	 * 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效
	 */
	private int certifiable;
	
	/**
	 * 证书无效原因码
	 */
	private int errCode;
	
	/**
	 * 证书终止有效期
	 */
	private String endTime;
	
	/**
	 * 保密属性 缺省为0; 0:不涉密, 1:涉密
	 */
	private String secrecy;
	
	/**
	 * IP地址
	 */
	private String ipAddress;
	
	/**
	 * 端口号
	 */
	private int port;
	
	/**
	 * 密码
	 */
	private String password;

	/**
	 * 云台类型
	 */
	private int PTZType;

	/**
	 * 云台类型描述字符串
	 */
	private String PTZTypeText;
	
	/**
	 * 在线/离线
	 * 1在线,0离线
	 * 默认在线
	 * 信令:
	 * <Status>ON</Status>
	 * <Status>OFF</Status>
	 * 遇到过NVR下的IPC下发信令可以推流， 但是 Status 响应 OFF
	 */
	private int status;

	/**
	 * 经度
	 */
	private double longitude;
	
	/**
	 * 纬度
	 */
	private double latitude;

	/**
	 * 子设备数
	 */
	private int subCount;

	/**
	 * 流唯一编号，存在表示正在直播
	 */
	private String  ssrc;

	/**
	 *  是否含有音频
	 */
	private  boolean hasAudio;

	/**
	 *  是否正在播放
	 */
	private  boolean play;


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
}
