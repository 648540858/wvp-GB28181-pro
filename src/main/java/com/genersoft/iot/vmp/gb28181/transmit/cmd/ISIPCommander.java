package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.gb28181.bean.Device;

/**    
 * @Description:设备能力接口，用于定义设备的控制、查询能力   
 * @author: songww
 * @date:   2020年5月3日 下午9:16:34     
 */
public interface ISIPCommander {

	/**
	 * 云台方向放控制，使用配置文件中的默认镜头移动速度
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
	 */
	public boolean ptzdirectCmd(String deviceId,String channelId,int leftRight, int upDown);
	
	/**
	 * 云台方向放控制
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
	 */
	public boolean ptzdirectCmd(String deviceId,String channelId,int leftRight, int upDown, int moveSpeed);
	
	/**
	 * 云台缩放控制，使用配置文件中的默认镜头缩放速度
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
	 */
	public boolean ptzZoomCmd(String deviceId,String channelId,int inOut);
	
	/**
	 * 云台缩放控制
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param zoomSpeed  镜头缩放速度
	 */
	public boolean ptzZoomCmd(String deviceId,String channelId,int inOut, int moveSpeed);
	
	/**
	 * 云台控制，支持方向与缩放控制
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度
     * @param zoomSpeed  镜头缩放速度
	 */
	public boolean ptzCmd(String deviceId,String channelId,int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed);
	
	/**
	 * 请求预览视频流
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */
	public String playStreamCmd(String deviceId,String channelId);
	
	/**
	 * 语音广播
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */
	public String audioBroadcastCmd(String deviceId,String channelId);
	
	/**
	 * 音视频录像控制
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */
	public String recordCmd(String deviceId,String channelId);
	
	/**
	 * 报警布防/撤防命令
	 * 
	 * @param deviceId  视频设备
	 */
	public String guardCmd(String deviceId);
	
	/**
	 * 报警复位命令
	 * 
	 * @param deviceId  视频设备
	 */
	public String alarmCmd(String deviceId);
	
	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */
	public String iFameCmd(String deviceId,String channelId);
	
	/**
	 * 看守位控制命令
	 * 
	 * @param deviceId  视频设备
	 */
	public String homePositionCmd(String deviceId);
	
	/**
	 * 设备配置命令
	 * 
	 * @param deviceId  视频设备
	 */
	public String deviceConfigCmd(String deviceId);
	
	
	/**
	 * 查询设备状态
	 * 
	 * @param device 视频设备
	 */
	public boolean deviceStatusQuery(Device device);
	
	/**
	 * 查询设备信息
	 * 
	 * @param device 视频设备
	 * @return 
	 */
	public boolean deviceInfoQuery(Device device);
	
	/**
	 * 查询目录列表
	 * 
	 * @param device 视频设备
	 */
	public boolean catalogQuery(Device device);
	
	/**
	 * 查询录像信息
	 * 
	 * @param device 视频设备
	 */
	public boolean recordInfoQuery(Device device);
	
	/**
	 * 查询报警信息
	 * 
	 * @param device 视频设备
	 */
	public boolean alarmInfoQuery(Device device);
	
	/**
	 * 查询设备配置
	 * 
	 * @param device 视频设备
	 */
	public boolean configQuery(Device device);
	
	/**
	 * 查询设备预置位置
	 * 
	 * @param device 视频设备
	 */
	public boolean presetQuery(Device device);
	
	/**
	 * 查询移动设备位置数据
	 * 
	 * @param device 视频设备
	 */
	public boolean mobilePostitionQuery(Device device);
}
