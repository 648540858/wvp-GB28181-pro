package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import gov.nist.javax.sip.message.SIPRequest;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * @description:设备能力接口，用于定义设备的控制、查询能力
 * @author: swwheihei
 * @date:   2020年5月3日 下午9:16:34
 */
public interface ISIPCommander {

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
	void ptzCmd(Device device,String channelId,int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) throws InvalidArgumentException, SipException, ParseException;

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
	void frontEndCmd(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) throws SipException, InvalidArgumentException, ParseException;

	/**
	 * 前端控制指令（用于转发上级指令）
	 * @param device		控制设备
	 * @param channelId		预览通道
	 * @param cmdString		前端控制指令串
	 */
	void fronEndCmd(Device device, String channelId, String cmdString, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 请求预览视频流
	 * @param device  视频设备
	 * @param channel  预览通道
	 */
	void playStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 请求回放视频流
	 *
	 * @param device  视频设备
	 * @param channel  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 */
	void playbackStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInf, Device device, DeviceChannel channel, String startTime, String endTime, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 请求历史媒体下载
	 *
	 * @param device  视频设备
	 * @param channel  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param downloadSpeed 下载倍速参数
	 */
	void downloadStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                           String startTime, String endTime, int downloadSpeed,
                           SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;


	/**
	 * 视频流停止
	 */
	void streamByeCmd(Device device, String channelId, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

	void talkStreamCmd(MediaServer mediaServerItem, SendRtpInfo sendRtpItem, Device device, DeviceChannel channelId, String callId, HookSubscribe.Event event, HookSubscribe.Event eventForPush, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

	void streamByeCmd(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

	/**
	 * 回放暂停
	 */
	void playPauseCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放恢复
	 */
	void playResumeCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放拖动播放
	 */
	void playSeekCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放倍速播放
	 */
	void playSpeedCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放控制
	 * @param device
	 * @param streamInfo
	 * @param content
	 */
	void playbackControlCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, String content,SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException;


    void streamByeCmdForDeviceInvite(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    /**
	 * /**
	 * 语音广播
	 *
	 * @param device 视频设备
	 */
	void audioBroadcastCmd(Device device, String channelId, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 音视频录像控制
	 *
	 * @param device  		视频设备
	 * @param channelId  	预览通道
	 * @param recordCmdStr	录像命令：Record / StopRecord
	 */
	void recordCmd(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 远程启动控制命令
	 *
	 * @param device	视频设备
	 */
	void teleBootCmd(Device device) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 报警布防/撤防命令
	 *
	 * @param device  	视频设备
	 */
	void guardCmd(Device device, String guardCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 报警复位命令
	 *
	 * @param device		视频设备
	 * @param alarmMethod	报警方式（可选）
	 * @param alarmType		报警类型（可选）
	 */
	void alarmResetCmd(Device device, String alarmMethod, String alarmType, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 *
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */
	void iFrameCmd(Device device, String channelId) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 看守位控制命令
	 *
	 */
	void homePositionCmd(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 设备配置命令
	 *
	 * @param device  视频设备
	 */
	void deviceConfigCmd(Device device);

	/**
	 * 设备配置命令：basicParam
	 */
	void deviceBasicConfigCmd(Device device, BasicParam basicParam, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备状态
	 *
	 * @param device 视频设备
	 */
	void deviceStatusQuery(Device device, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备信息
	 *
	 * @param device   视频设备
	 * @param callback
	 * @return
	 */
	void deviceInfoQuery(Device device, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询目录列表
	 *
	 * @param device 视频设备
	 */
	void catalogQuery(Device device, int sn, SipSubscribe.Event errorEvent) throws SipException, InvalidArgumentException, ParseException;

	/**
	 * 查询录像信息
	 *
	 * @param device 视频设备
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param sn
	 */
	void recordInfoQuery(Device device, String channelId, String startTime, String endTime, int sn,  Integer Secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

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
	void alarmInfoQuery(Device device, String startPriority, String endPriority, String alarmMethod,
							String alarmType, String startTime, String endTime,  ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备配置
	 *
	 * @param device 		视频设备
	 * @param channelId		通道编码（可选）
	 * @param configType	配置类型：
	 */
	void deviceConfigQuery(Device device, String channelId, String configType,  ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备预置位置
	 *
	 * @param device 视频设备
	 */
	void presetQuery(Device device, String channelId, ErrorCallback<List<Preset>> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询移动设备位置数据
	 *
	 * @param device 视频设备
	 */
	void mobilePostitionQuery(Device device, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 订阅、取消订阅移动位置
	 *
	 * @param device	视频设备
	 * @return			true = 命令发送成功
	 */
	SIPRequest mobilePositionSubscribe(Device device, SipTransactionInfo transactionInfo, SipSubscribe.Event okEvent , SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 订阅、取消订阅报警信息
	 * @param device		视频设备
	 * @param expires		订阅过期时间（0 = 取消订阅）
	 * @param startPriority	报警起始级别（可选）
	 * @param endPriority	报警终止级别（可选）
	 * @param startTime		报警发生起始时间（可选）
	 * @param endTime		报警发生终止时间（可选）
	 * @return				true = 命令发送成功
	 */
	void alarmSubscribe(Device device, int expires, String startPriority, String endPriority, String alarmMethod, String startTime, String endTime) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 订阅、取消订阅目录信息
	 * @param device		视频设备
	 * @return				true = 命令发送成功
	 */
	SIPRequest catalogSubscribe(Device device, SipTransactionInfo transactionInfo, SipSubscribe.Event okEvent ,SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 拉框控制命令
	 *
	 * @param device    控制设备
	 * @param channelId 通道id
	 * @param cmdString 前端控制指令串
	 */
	void dragZoomCmd(Device device, String channelId, String cmdString, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;


    void playbackControlCmd(Device device, DeviceChannel channel, String stream, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
	 * 向设备发送报警NOTIFY消息， 用于互联结构下，此时将设备当成一个平级平台看待
	 * @param device 设备
	 * @param deviceAlarm 报警信息信息
	 * @return
	 */
	void sendAlarmMessage(Device device, DeviceAlarm deviceAlarm) throws InvalidArgumentException, SipException, ParseException;


}
