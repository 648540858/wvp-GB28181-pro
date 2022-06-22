package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

import javax.sip.Dialog;

/**    
 * @description:设备能力接口，用于定义设备的控制、查询能力   
 * @author: swwheihei
 * @date:   2020年5月3日 下午9:16:34     
 */
public interface ISIPCommander {

	/**
	 * 云台方向放控制，使用配置文件中的默认镜头移动速度
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
	 */
	boolean ptzdirectCmd(Device device,String channelId,int leftRight, int upDown);
	
	/**
	 * 云台方向放控制
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
	 */
	boolean ptzdirectCmd(Device device,String channelId,int leftRight, int upDown, int moveSpeed);
	
	/**
	 * 云台缩放控制，使用配置文件中的默认镜头缩放速度
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
	 */
	boolean ptzZoomCmd(Device device,String channelId,int inOut);
	
	/**
	 * 云台缩放控制
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
	 */
	boolean ptzZoomCmd(Device device,String channelId,int inOut, int moveSpeed);
	
	/**
	 * 云台控制，支持方向与缩放控制
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度
     * @param zoomSpeed  镜头缩放速度
	 */
	boolean ptzCmd(Device device,String channelId,int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed);
	
	/**
	 * 前端控制，包括PTZ指令、FI指令、预置位指令、巡航指令、扫描指令和辅助开关指令
	 * 
	 * @param device  		控制设备
	 * @param channelId		预览通道
	 * @param cmdCode		指令码
     * @param parameter1	数据1
     * @param parameter2	数据2
     * @param combineCode2	组合码2
	 */
	boolean frontEndCmd(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2);
	
	/**
	 * 前端控制指令（用于转发上级指令）
	 * @param device		控制设备
	 * @param channelId		预览通道
	 * @param cmdString		前端控制指令串
	 */
	boolean fronEndCmd(Device device, String channelId, String cmdString, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent);

	/**
	 * 请求预览视频流
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */
	void playStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent);

	/**
	 * 请求回放视频流
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 */
	void playbackStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInf, Device device, String channelId, String startTime, String endTime,InviteStreamCallback inviteStreamCallback, InviteStreamCallback event, SipSubscribe.Event errorEvent);

	/**
	 * 请求历史媒体下载
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param downloadSpeed 下载倍速参数
	 */ 
	void downloadStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
						   String startTime, String endTime, int downloadSpeed, InviteStreamCallback inviteStreamCallback, InviteStreamCallback hookEvent,
						   SipSubscribe.Event errorEvent);

	/**
	 * 视频流停止
	 */
	void streamByeCmd(String deviceId, String channelId, String stream, String callId, SipSubscribe.Event okEvent);
	void streamByeCmd(String deviceId, String channelId, String stream, String callId);

	/**
	 * 回放暂停
	 */
	void playPauseCmd(Device device, StreamInfo streamInfo);

	/**
	 * 回放恢复
	 */
	void playResumeCmd(Device device, StreamInfo streamInfo);

	/**
	 * 回放拖动播放
	 */
	void playSeekCmd(Device device, StreamInfo streamInfo, long seekTime);

	/**
	 * 回放倍速播放
	 */
	void playSpeedCmd(Device device, StreamInfo streamInfo, Double speed);
	
	/**
	 * 回放控制
	 * @param device
	 * @param streamInfo
	 * @param content
	 */
	void playbackControlCmd(Device device, StreamInfo streamInfo, String content,SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent);

	/**
	 * 语音广播
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */
	boolean audioBroadcastCmd(Device device,String channelId);
	
	/**
	 * 语音广播
	 * 
	 * @param device  视频设备
	 */
	void audioBroadcastCmd(Device device, SipSubscribe.Event okEvent);
	boolean audioBroadcastCmd(Device device);
	
	/**
	 * 音视频录像控制
	 * 
	 * @param device  		视频设备
	 * @param channelId  	预览通道
	 * @param recordCmdStr	录像命令：Record / StopRecord
	 */
	boolean recordCmd(Device device, String channelId, String recordCmdStr, SipSubscribe.Event errorEvent);
	
	/**
	 * 远程启动控制命令
	 * 
	 * @param device	视频设备
	 */
	boolean teleBootCmd(Device device);

	/**
	 * 报警布防/撤防命令
	 * 
	 * @param device  	视频设备
	 */
	boolean guardCmd(Device device, String guardCmdStr, SipSubscribe.Event errorEvent);
	
	/**
	 * 报警复位命令
	 * 
	 * @param device		视频设备
	 * @param alarmMethod	报警方式（可选）
	 * @param alarmType		报警类型（可选）
	 */
	boolean alarmCmd(Device device, String alarmMethod, String alarmType, SipSubscribe.Event errorEvent);
	
	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */
	boolean iFrameCmd(Device device, String channelId);
	
	/**
	 * 看守位控制命令
	 * 
	 * @param device		视频设备
	 * @param enabled		看守位使能：1 = 开启，0 = 关闭
	 * @param resetTime		自动归位时间间隔，开启看守位时使用，单位:秒(s)
	 * @param presetIndex	调用预置位编号，开启看守位时使用，取值范围0~255
	 */
	boolean homePositionCmd(Device device, String channelId, String enabled, String resetTime, String presetIndex, SipSubscribe.Event errorEvent);
	
	/**
	 * 设备配置命令
	 * 
	 * @param device  视频设备
	 */
	boolean deviceConfigCmd(Device device);
	
		/**
	 * 设备配置命令：basicParam
	 * 
	 * @param device  			视频设备
	 * @param channelId			通道编码（可选）
	 * @param name				设备/通道名称（可选）
	 * @param expiration		注册过期时间（可选）
	 * @param heartBeatInterval	心跳间隔时间（可选）
	 * @param heartBeatCount	心跳超时次数（可选）
	 */  
	boolean deviceBasicConfigCmd(Device device, String channelId, String name, String expiration, String heartBeatInterval, String heartBeatCount, SipSubscribe.Event errorEvent);
	
	/**
	 * 查询设备状态
	 * 
	 * @param device 视频设备
	 */
	boolean deviceStatusQuery(Device device, SipSubscribe.Event errorEvent);
	
	/**
	 * 查询设备信息
	 * 
	 * @param device 视频设备
	 * @return 
	 */
	boolean deviceInfoQuery(Device device);
	
	/**
	 * 查询目录列表
	 * 
	 * @param device 视频设备
	 */
	boolean catalogQuery(Device device, int sn, SipSubscribe.Event errorEvent);
	
	/**
	 * 查询录像信息
	 * 
	 * @param device 视频设备
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param sn
	 */
	boolean recordInfoQuery(Device device, String channelId, String startTime, String endTime, int sn,  Integer Secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent);
	
	/**
	 * 查询报警信息
	 * 
	 * @param device		视频设备
	 * @param startPriority	报警起始级别（可选）
	 * @param endPriority	报警终止级别（可选）
	 * @param alarmMethod	报警方式条件（可选）
	 * @param alarmType		报警类型
	 * @param startTime		报警发生起始时间（可选）
	 * @param endTime		报警发生终止时间（可选）
	 * @return				true = 命令发送成功
	 */
	boolean alarmInfoQuery(Device device, String startPriority, String endPriority, String alarmMethod,
							String alarmType, String startTime, String endTime, SipSubscribe.Event errorEvent);	
	
	/**
	 * 查询设备配置
	 * 
	 * @param device 		视频设备
	 * @param channelId		通道编码（可选）
	 * @param configType	配置类型：
	 */
	boolean deviceConfigQuery(Device device, String channelId, String configType,  SipSubscribe.Event errorEvent);
	
	/**
	 * 查询设备预置位置
	 * 
	 * @param device 视频设备
	 */
	boolean presetQuery(Device device, String channelId, SipSubscribe.Event errorEvent);
	
	/**
	 * 查询移动设备位置数据
	 * 
	 * @param device 视频设备
	 */
	boolean mobilePostitionQuery(Device device, SipSubscribe.Event errorEvent);

	/**
	 * 订阅、取消订阅移动位置
	 * 
	 * @param device	视频设备
	 * @return			true = 命令发送成功
	 */
	boolean mobilePositionSubscribe(Device device, Dialog dialog, SipSubscribe.Event okEvent , SipSubscribe.Event errorEvent);

	/**
	 * 订阅、取消订阅报警信息
	 * @param device		视频设备
	 * @param expires		订阅过期时间（0 = 取消订阅）
	 * @param startPriority	报警起始级别（可选）
	 * @param endPriority	报警终止级别（可选）
	 * @param alarmType		报警类型
	 * @param startTime		报警发生起始时间（可选）
	 * @param endTime		报警发生终止时间（可选）
	 * @return				true = 命令发送成功
	 */
	boolean alarmSubscribe(Device device, int expires, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime);

	/**
	 * 订阅、取消订阅目录信息
	 * @param device		视频设备
	 * @return				true = 命令发送成功
	 */
	boolean catalogSubscribe(Device device, Dialog dialog, SipSubscribe.Event okEvent ,SipSubscribe.Event errorEvent);

	/**
	 * 拉框控制命令
	 *
	 * @param device    控制设备
	 * @param channelId 通道id
	 * @param cmdString 前端控制指令串
	 */
	boolean dragZoomCmd(Device device, String channelId, String cmdString);


	/**
	 * 向设备发送报警NOTIFY消息， 用于互联结构下，此时将设备当成一个平级平台看待
	 * @param device 设备
	 * @param deviceAlarm 报警信息信息
	 * @return
	 */
	boolean sendAlarmMessage(Device device, DeviceAlarm deviceAlarm);
}
