package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**    
 * @description:设备能力接口，用于定义设备的控制、查询能力   
 * @author: swwheihei
 * @date:   2020年5月3日 下午9:22:48     
 */
@Component
@DependsOn("sipLayer")
public class SIPCommander implements ISIPCommander {

	private final Logger logger = LoggerFactory.getLogger(SIPCommander.class);

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private SipFactory sipFactory;

	@Autowired
	@Qualifier(value="tcpSipProvider")
	private SipProviderImpl tcpSipProvider;

	@Autowired
	@Qualifier(value="udpSipProvider")
	private SipProviderImpl udpSipProvider;

	@Autowired
	private SIPRequestHeaderProvider headerProvider;
	
	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private ZLMHttpHookSubscribe subscribe;

	@Autowired
	private SipSubscribe sipSubscribe;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private DynamicTask dynamicTask;


	/**
	 * 云台方向放控制，使用配置文件中的默认镜头移动速度
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
	 */
	@Override
	public boolean ptzdirectCmd(Device device, String channelId, int leftRight, int upDown) {
		return ptzCmd(device, channelId, leftRight, upDown, 0, sipConfig.getPtzSpeed(), 0);
	}

	/**
	 * 云台方向放控制
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
	 */
	@Override
	public boolean ptzdirectCmd(Device device, String channelId, int leftRight, int upDown, int moveSpeed) {
		return ptzCmd(device, channelId, leftRight, upDown, 0, moveSpeed, 0);
	}

	/**
	 * 云台缩放控制，使用配置文件中的默认镜头缩放速度
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
	 */  
	@Override
	public boolean ptzZoomCmd(Device device, String channelId, int inOut) {
		return ptzCmd(device, channelId, 0, 0, inOut, 0, sipConfig.getPtzSpeed());
	}

	/**
	 * 云台缩放控制
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param zoomSpeed  镜头缩放速度
	 */ 
	@Override
	public boolean ptzZoomCmd(Device device, String channelId, int inOut, int zoomSpeed) {
		return ptzCmd(device, channelId, 0, 0, inOut, 0, zoomSpeed);
	}
  
   /**
	* 云台指令码计算 
	*
    * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
    * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
    * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
    * @param moveSpeed  镜头移动速度 默认 0XFF (0-255)
    * @param zoomSpeed  镜头缩放速度 默认 0X1 (0-255)
    */
    public static String cmdString(int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) {
		int cmdCode = 0;
		if (leftRight == 2) {
			cmdCode|=0x01;		// 右移
		} else if(leftRight == 1) {
			cmdCode|=0x02;		// 左移
		}
		if (upDown == 2) {
			cmdCode|=0x04;		// 下移
		} else if(upDown == 1) {
			cmdCode|=0x08;		// 上移
		}
		if (inOut == 2) {
			cmdCode |= 0x10;	// 放大
		} else if(inOut == 1) {
			cmdCode |= 0x20;	// 缩小
		}
		StringBuilder builder = new StringBuilder("A50F01");
		String strTmp;
		strTmp = String.format("%02X", cmdCode);
		builder.append(strTmp, 0, 2);
		strTmp = String.format("%02X", moveSpeed);
		builder.append(strTmp, 0, 2);
		builder.append(strTmp, 0, 2);
		strTmp = String.format("%X", zoomSpeed);
		builder.append(strTmp, 0, 1).append("0");
		//计算校验码
		int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + moveSpeed + moveSpeed + (zoomSpeed /*<< 4*/ & 0XF0)) % 0X100;
		strTmp = String.format("%02X", checkCode);
		builder.append(strTmp, 0, 2);
		return builder.toString();
}

   /**
	* 云台指令码计算 
	*
	 * @param cmdCode 		指令码
	 * @param parameter1	数据1
	 * @param parameter2	数据2
	 * @param combineCode2	组合码2
	 */
    public static String frontEndCmdString(int cmdCode, int parameter1, int parameter2, int combineCode2) {
		StringBuilder builder = new StringBuilder("A50F01");
		String strTmp;
		strTmp = String.format("%02X", cmdCode);
		builder.append(strTmp, 0, 2);
		strTmp = String.format("%02X", parameter1);
		builder.append(strTmp, 0, 2);
		strTmp = String.format("%02X", parameter2);
		builder.append(strTmp, 0, 2);
		strTmp = String.format("%X", combineCode2);
		builder.append(strTmp, 0, 1).append("0");
		//计算校验码
		int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + parameter1 + parameter2 + (combineCode2 & 0XF0)) % 0X100;
		strTmp = String.format("%02X", checkCode);
		builder.append(strTmp, 0, 2);
		return builder.toString();
	}

	/**
	 * 云台控制，支持方向与缩放控制
	 * 
	 * @param device  	控制设备
	 * @param channelId	预览通道
	 * @param leftRight	镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown	镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut		镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed	镜头移动速度
     * @param zoomSpeed	镜头缩放速度
	 */
	@Override
	public boolean ptzCmd(Device device, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
			int zoomSpeed) {
		try {
			String cmdStr= cmdString(leftRight, upDown, inOut, moveSpeed, zoomSpeed);
			StringBuffer ptzXml = new StringBuffer(200);
			String charset = device.getCharset();
			ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			ptzXml.append("<Control>\r\n");
			ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			ptzXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
			ptzXml.append("<Info>\r\n");
			ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
			ptzXml.append("</Info>\r\n");
			ptzXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), "z9hG4bK-ViaPtz-" + tm, "FromPtz" + tm, null, callIdHeader);
			
			transmitRequest(device, request);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
		return false;
	}

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
	@Override
	public boolean frontEndCmd(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) {
		try {
			String cmdStr= frontEndCmdString(cmdCode, parameter1, parameter2, combineCode2);
			logger.debug("控制字符串：" + cmdStr);
			StringBuffer ptzXml = new StringBuffer(200);
			String charset = device.getCharset();
			ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			ptzXml.append("<Control>\r\n");
			ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			ptzXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
			ptzXml.append("<Info>\r\n");
			ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
			ptzXml.append("</Info>\r\n");
			ptzXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), "z9hG4bK-ViaPtz-" + tm, "FromPtz" + tm, null, callIdHeader);
			transmitRequest(device, request);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
		return false;
	}

	/**
	 * 前端控制指令（用于转发上级指令）
	 * @param device		控制设备
	 * @param channelId		预览通道
	 * @param cmdString		前端控制指令串
	 */
	@Override
	public boolean fronEndCmd(Device device, String channelId, String cmdString, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) {
		try {
			StringBuffer ptzXml = new StringBuffer(200);
			String charset = device.getCharset();
			ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			ptzXml.append("<Control>\r\n");
			ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			ptzXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			ptzXml.append("<PTZCmd>" + cmdString + "</PTZCmd>\r\n");
			ptzXml.append("<Info>\r\n");
			ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
			ptzXml.append("</Info>\r\n");
			ptzXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), "z9hG4bK-ViaPtz-" + tm, "FromPtz" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent, okEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
		return false;
	}
	
	 /**
	 * 	请求预览视频流
	  * @param device  视频设备
	  * @param channelId  预览通道
	  * @param event hook订阅
	  * @param errorEvent sip错误订阅
	*/
	@Override
	public void playStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
							  ZLMHttpHookSubscribe.Event event, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) {
		String streamId = ssrcInfo.getStream();
		try {
			if (device == null) {
				return;
			}
			String streamMode = device.getStreamMode().toUpperCase();

			logger.info("{} 分配的ZLM为: {} [{}:{}]", streamId, mediaServerItem.getId(), mediaServerItem.getIp(), ssrcInfo.getPort());
			// 添加订阅
			JSONObject subscribeKey = new JSONObject();
			subscribeKey.put("app", "rtp");
			subscribeKey.put("stream", streamId);
			subscribeKey.put("regist", true);
			subscribeKey.put("schema", "rtmp");
			subscribeKey.put("mediaServerId", mediaServerItem.getId());
			subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey,
					(MediaServerItem mediaServerItemInUse, JSONObject json)->{
				if (event != null) {
					event.response(mediaServerItemInUse, json);
				}
			});
			//
			StringBuffer content = new StringBuffer(200);
			content.append("v=0\r\n");
			content.append("o="+ sipConfig.getId()+" 0 0 IN IP4 "+ mediaServerItem.getSdpIp() +"\r\n");
			content.append("s=Play\r\n");
			content.append("c=IN IP4 "+ mediaServerItem.getSdpIp() +"\r\n");
			content.append("t=0 0\r\n");

			if (userSetting.isSeniorSdp()) {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" RTP/AVP 96 126 125 99 34 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:126 H264/90000\r\n");
				content.append("a=rtpmap:125 H264S/90000\r\n");
				content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
				content.append("a=fmtp:99 profile-level-id=3\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}else {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" RTP/AVP 96 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if ("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				} else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}

			content.append("y="+ssrcInfo.getSsrc()+"\r\n");//ssrc
			// f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
//			content.append("f=v/2/5/25/1/4000a/1/8/1" + "\r\n"); // 未发现支持此特性的设备

			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createInviteRequest(device, channelId, content.toString(), null, "FromInvt" + tm, null, ssrcInfo.getSsrc(), callIdHeader);

			transmitRequest(device, request, (e -> {
				streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
				mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
				errorEvent.response(e);
			}), e ->{
				// 这里为例避免一个通道的点播只有一个callID这个参数使用一个固定值
				streamSession.put(device.getDeviceId(), channelId ,"play", streamId, ssrcInfo.getSsrc(), mediaServerItem.getId(), ((ResponseEvent)e.event).getClientTransaction(), VideoStreamSessionManager.SessionType.play);
				streamSession.put(device.getDeviceId(), channelId ,"play", e.dialog);
				okEvent.response(e);
			});

			
		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 请求回放视频流
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 */ 
	@Override
	public void playbackStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
								  String startTime, String endTime, InviteStreamCallback inviteStreamCallback, InviteStreamCallback hookEvent,
								  SipSubscribe.Event errorEvent) {
		try {

			logger.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getIp(), ssrcInfo.getPort());

			StringBuffer content = new StringBuffer(200);
	        content.append("v=0\r\n");
	        content.append("o="+sipConfig.getId()+" 0 0 IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
	        content.append("s=Playback\r\n");
	        content.append("u="+channelId+":0\r\n");
	        content.append("c=IN IP4 "+mediaServerItem.getSdpIp()+"\r\n");
	        content.append("t="+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime)+" "
					+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) +"\r\n");

			String streamMode = device.getStreamMode().toUpperCase();

			if (userSetting.isSeniorSdp()) {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" RTP/AVP 96 126 125 99 34 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:126 H264/90000\r\n");
				content.append("a=rtpmap:125 H264S/90000\r\n");
				content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
				content.append("a=fmtp:99 profile-level-id=3\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}else {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" RTP/AVP 96 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}

	        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
	        
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			// 添加订阅
			JSONObject subscribeKey = new JSONObject();
			subscribeKey.put("app", "rtp");
			subscribeKey.put("stream", ssrcInfo.getStream());
			subscribeKey.put("regist", true);
			subscribeKey.put("schema", "rtmp");
			subscribeKey.put("mediaServerId", mediaServerItem.getId());
			logger.debug("录像回放添加订阅，订阅内容：" + subscribeKey);
			subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey,
					(MediaServerItem mediaServerItemInUse, JSONObject json)->{
						if (hookEvent != null) {
							InviteStreamInfo inviteStreamInfo = new InviteStreamInfo(mediaServerItemInUse, json, callIdHeader.getCallId(), "rtp", ssrcInfo.getStream());
							hookEvent.call(inviteStreamInfo);
						}
					});
	        Request request = headerProvider.createPlaybackInviteRequest(device, channelId, content.toString(), null, "fromplybck" + tm, null, callIdHeader, ssrcInfo.getSsrc());

	        transmitRequest(device, request, errorEvent, okEvent -> {
				ResponseEvent responseEvent = (ResponseEvent) okEvent.event;
	        	streamSession.put(device.getDeviceId(), channelId, callIdHeader.getCallId(), ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerItem.getId(), responseEvent.getClientTransaction(), VideoStreamSessionManager.SessionType.playback);
				streamSession.put(device.getDeviceId(), channelId, callIdHeader.getCallId(), okEvent.dialog);
			});
			if (inviteStreamCallback != null) {
				inviteStreamCallback.call(new InviteStreamInfo(mediaServerItem, null, callIdHeader.getCallId(), "rtp", ssrcInfo.getStream()));
			}
		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 请求历史媒体下载
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param downloadSpeed 下载倍速参数
	 */ 
	@Override
	public void downloadStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
								  String startTime, String endTime, int downloadSpeed, InviteStreamCallback inviteStreamCallback, InviteStreamCallback hookEvent,
								  SipSubscribe.Event errorEvent) {
		try {
			logger.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getIp(), ssrcInfo.getPort());

			StringBuffer content = new StringBuffer(200);
	        content.append("v=0\r\n");
	        content.append("o="+sipConfig.getId()+" 0 0 IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
	        content.append("s=Download\r\n");
	        content.append("u="+channelId+":0\r\n");
	        content.append("c=IN IP4 "+mediaServerItem.getSdpIp()+"\r\n");
	        content.append("t="+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime)+" "
					+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) +"\r\n");

			String streamMode = device.getStreamMode().toUpperCase();

			if (userSetting.isSeniorSdp()) {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" RTP/AVP 96 126 125 99 34 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:126 H264/90000\r\n");
				content.append("a=rtpmap:125 H264S/90000\r\n");
				content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
				content.append("a=fmtp:99 profile-level-id=3\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}else {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" TCP/RTP/AVP 96 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ ssrcInfo.getPort() +" RTP/AVP 96 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}
			content.append("a=downloadspeed:" + downloadSpeed + "\r\n");

	        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
	        
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			// 添加订阅
			JSONObject subscribeKey = new JSONObject();
			subscribeKey.put("app", "rtp");
			subscribeKey.put("stream", ssrcInfo.getStream());
			subscribeKey.put("regist", true);
			subscribeKey.put("mediaServerId", mediaServerItem.getId());
			logger.debug("录像回放添加订阅，订阅内容：" + subscribeKey.toString());
			subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey,
					(MediaServerItem mediaServerItemInUse, JSONObject json)->{
						hookEvent.call(new InviteStreamInfo(mediaServerItem, json, callIdHeader.getCallId(), "rtp", ssrcInfo.getStream()));
						subscribe.removeSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey);
					});

	        Request request = headerProvider.createPlaybackInviteRequest(device, channelId, content.toString(), null, "fromplybck" + tm, null, callIdHeader, ssrcInfo.getSsrc());
			if (inviteStreamCallback != null) {
				inviteStreamCallback.call(new InviteStreamInfo(mediaServerItem, null, callIdHeader.getCallId(), "rtp", ssrcInfo.getStream()));
			}
	        transmitRequest(device, request, errorEvent, okEvent->{
				ResponseEvent responseEvent = (ResponseEvent) okEvent.event;
				streamSession.put(device.getDeviceId(), channelId, callIdHeader.getCallId(), ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerItem.getId(), responseEvent.getClientTransaction(), VideoStreamSessionManager.SessionType.download);
				streamSession.put(device.getDeviceId(), channelId, callIdHeader.getCallId(), okEvent.dialog);
			});


		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 视频流停止, 不使用回调
	 */
	@Override
	public void streamByeCmd(String deviceId, String channelId, String stream, String callId) {
		streamByeCmd(deviceId, channelId, stream, callId, null);
	}

	/**
	 * 视频流停止
	 */
	@Override
	public void streamByeCmd(String deviceId, String channelId, String stream, String callId, SipSubscribe.Event okEvent) {
		try {
			SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(deviceId, channelId, null, stream);
			ClientTransaction transaction = streamSession.getTransactionByStream(deviceId, channelId, stream);

			if (transaction == null) {
				logger.warn("[ {} -> {}]停止视频流的时候发现事务已丢失", deviceId, channelId);
				SipSubscribe.EventResult<Object> eventResult = new SipSubscribe.EventResult<>();
				if (okEvent != null) {
					okEvent.response(eventResult);
				}
				return;
			}
			SIPDialog dialog;
			if (callId != null) {
				dialog = streamSession.getDialogByCallId(deviceId, channelId, callId);
			}else {
				if (stream == null) {
					return;
				}
				dialog = streamSession.getDialogByStream(deviceId, channelId, stream);
			}
			if (ssrcTransaction != null) {
				MediaServerItem mediaServerItem = mediaServerService.getOne(ssrcTransaction.getMediaServerId());
				mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcTransaction.getSsrc());
				mediaServerService.closeRTPServer(deviceId, channelId, ssrcTransaction.getStream());
				streamSession.remove(deviceId, channelId, ssrcTransaction.getStream());
			}

			if (dialog == null) {
				logger.warn("[ {} -> {}]停止视频流的时候发现对话已丢失", deviceId, channelId);
				return;
			}
			SipStack sipStack = udpSipProvider.getSipStack();
			SIPDialog sipDialog = ((SipStackImpl) sipStack).putDialog(dialog);
			if (dialog != sipDialog) {
				dialog = sipDialog;
			}else {
				dialog.setSipProvider(udpSipProvider);
				try {
					Field sipStackField = SIPDialog.class.getDeclaredField("sipStack");
					sipStackField.setAccessible(true);
					sipStackField.set(dialog, sipStack);
					Field eventListenersField = SIPDialog.class.getDeclaredField("eventListeners");
					eventListenersField.setAccessible(true);
					eventListenersField.set(dialog, new HashSet<>());
				} catch (NoSuchFieldException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}

			Request byeRequest = dialog.createRequest(Request.BYE);
			SipURI byeURI = (SipURI) byeRequest.getRequestURI();
			SIPRequest request = (SIPRequest)transaction.getRequest();
			byeURI.setHost(request.getRemoteAddress().getHostAddress());
			byeURI.setPort(request.getRemotePort());
			ViaHeader viaHeader = (ViaHeader) byeRequest.getHeader(ViaHeader.NAME);
			String protocol = viaHeader.getTransport().toUpperCase();
			ClientTransaction clientTransaction = null;
			if("TCP".equals(protocol)) {
				clientTransaction = tcpSipProvider.getNewClientTransaction(byeRequest);
			} else if("UDP".equals(protocol)) {
				clientTransaction = udpSipProvider.getNewClientTransaction(byeRequest);
			}

			CallIdHeader callIdHeader = (CallIdHeader) byeRequest.getHeader(CallIdHeader.NAME);
			if (okEvent != null) {
				sipSubscribe.addOkSubscribe(callIdHeader.getCallId(), okEvent);
			}

			dialog.sendRequest(clientTransaction);

		} catch (SipException | ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 语音广播
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */
	@Override
	public boolean audioBroadcastCmd(Device device, String channelId) {
		// 改为新的实现
		return false;
	}

	/**
	 * 语音广播
	 * 
	 * @param device  视频设备
	 */
	@Override
	public boolean audioBroadcastCmd(Device device) {
		try {
			StringBuffer broadcastXml = new StringBuffer(200);
			String charset = device.getCharset();
			broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			broadcastXml.append("<Notify>\r\n");
			broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
			broadcastXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
			broadcastXml.append("<TargetID>" + device.getDeviceId() + "</TargetID>\r\n");
			broadcastXml.append("</Notify>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());
			
			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();
								
			Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), "z9hG4bK-ViaBcst-" + tm, "FromBcst" + tm, null, callIdHeader);
			transmitRequest(device, request);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
		return false;
	}
	@Override
	public void audioBroadcastCmd(Device device, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer broadcastXml = new StringBuffer(200);
			String charset = device.getCharset();
			broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			broadcastXml.append("<Notify>\r\n");
			broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
			broadcastXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
			broadcastXml.append("<TargetID>" + device.getDeviceId() + "</TargetID>\r\n");
			broadcastXml.append("</Notify>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();
								
			Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), "z9hG4bK-ViaBcst-" + tm, "FromBcst" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
	} 
	
	
	/**
	 * 音视频录像控制
	 * 
	 * @param device		视频设备
	 * @param channelId  	预览通道
	 * @param recordCmdStr	录像命令：Record / StopRecord
	 */  
	@Override
	public boolean recordCmd(Device device, String channelId, String recordCmdStr, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			cmdXml.append("<RecordCmd>" + recordCmdStr + "</RecordCmd>\r\n");
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromRecord" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 远程启动控制命令
	 * 
	 * @param device	视频设备
	 */
	@Override
	public boolean teleBootCmd(Device device) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			cmdXml.append("<TeleBoot>Boot</TeleBoot>\r\n");
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromBoot" + tm, null, callIdHeader);
			transmitRequest(device, request);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}
	
	/**
	 * 报警布防/撤防命令
	 * 
	 * @param device  		视频设备
	 * @param guardCmdStr	"SetGuard"/"ResetGuard"
	 */
	@Override
	public boolean guardCmd(Device device, String guardCmdStr, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			cmdXml.append("<GuardCmd>" + guardCmdStr + "</GuardCmd>\r\n");
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromGuard" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 报警复位命令
	 * 
	 * @param device  视频设备
	 */  
	@Override
	public boolean alarmCmd(Device device, String alarmMethod, String alarmType, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			cmdXml.append("<AlarmCmd>ResetAlarm</AlarmCmd>\r\n");
			if (!StringUtils.isEmpty(alarmMethod) || !StringUtils.isEmpty(alarmType)) {
				cmdXml.append("<Info>\r\n");
			}
			if (!StringUtils.isEmpty(alarmMethod)) {
				cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
			}
			if (!StringUtils.isEmpty(alarmType)) {
				cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
			}
			if (!StringUtils.isEmpty(alarmMethod) || !StringUtils.isEmpty(alarmType)) {
				cmdXml.append("</Info>\r\n");
			}
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromAlarm" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */ 
	@Override
	public boolean iFrameCmd(Device device, String channelId) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			cmdXml.append("<IFameCmd>Send</IFameCmd>\r\n");
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromBoot" + tm, null, callIdHeader);
			transmitRequest(device, request);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 看守位控制命令
	 * 
	 * @param device		视频设备
	 * @param enabled		看守位使能：1 = 开启，0 = 关闭
	 * @param resetTime		自动归位时间间隔，开启看守位时使用，单位:秒(s)
	 * @param presetIndex	调用预置位编号，开启看守位时使用，取值范围0~255
	 */  
	@Override
	public boolean homePositionCmd(Device device, String channelId, String enabled, String resetTime, String presetIndex, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			cmdXml.append("<HomePosition>\r\n");
			if (NumericUtil.isInteger(enabled) && (!enabled.equals("0"))) {
				cmdXml.append("<Enabled>1</Enabled>\r\n");
				if (NumericUtil.isInteger(resetTime)) {
					cmdXml.append("<ResetTime>" + resetTime + "</ResetTime>\r\n");
				} else {
					cmdXml.append("<ResetTime>0</ResetTime>\r\n");
				}
				if (NumericUtil.isInteger(presetIndex)) {
					cmdXml.append("<PresetIndex>" + presetIndex + "</PresetIndex>\r\n");
				} else {
					cmdXml.append("<PresetIndex>0</PresetIndex>\r\n");
				}
			} else {
				cmdXml.append("<Enabled>0</Enabled>\r\n");
			}
			cmdXml.append("</HomePosition>\r\n");
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromGuard" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 设备配置命令
	 * 
	 * @param device  视频设备
	 */  
	@Override
	public boolean deviceConfigCmd(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

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
	@Override
	public boolean deviceBasicConfigCmd(Device device, String channelId, String name, String expiration, 
										String heartBeatInterval, String heartBeatCount, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Control>\r\n");
			cmdXml.append("<CmdType>DeviceConfig</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			cmdXml.append("<BasicParam>\r\n");
			if (!StringUtils.isEmpty(name)) {
				cmdXml.append("<Name>" + name + "</Name>\r\n");
			}
			if (NumericUtil.isInteger(expiration)) {
				if (Integer.valueOf(expiration) > 0) {
					cmdXml.append("<Expiration>" + expiration + "</Expiration>\r\n");
				}
			}
			if (NumericUtil.isInteger(heartBeatInterval)) {
				if (Integer.valueOf(heartBeatInterval) > 0) {
					cmdXml.append("<HeartBeatInterval>" + heartBeatInterval + "</HeartBeatInterval>\r\n");
				}
			}
			if (NumericUtil.isInteger(heartBeatCount)) {
				if (Integer.valueOf(heartBeatCount) > 0) {
					cmdXml.append("<HeartBeatCount>" + heartBeatCount + "</HeartBeatCount>\r\n");
				}
			}
			cmdXml.append("</BasicParam>\r\n");
			cmdXml.append("</Control>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromConfig" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 查询设备状态
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean deviceStatusQuery(Device device, SipSubscribe.Event errorEvent) {
		try {
			String charset = device.getCharset();
			StringBuffer catalogXml = new StringBuffer(200);
			catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			catalogXml.append("<Query>\r\n");
			catalogXml.append("<CmdType>DeviceStatus</CmdType>\r\n");
			catalogXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			catalogXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), null, "FromStatus" + tm, null, callIdHeader);

			transmitRequest(device, request, errorEvent);
			return true;
			
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 查询设备信息
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean deviceInfoQuery(Device device) {
		try {
			StringBuffer catalogXml = new StringBuffer(200);
			String charset = device.getCharset();
			catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			catalogXml.append("<Query>\r\n");
			catalogXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
			catalogXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			catalogXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), "z9hG4bK-ViaDeviceInfo-" + tm, "FromDev" + tm, null, callIdHeader);

			transmitRequest(device, request);
			
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 查询目录列表
	 * 
	 * @param device 视频设备
	 */ 
	@Override
	public boolean catalogQuery(Device device, int sn, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer catalogXml = new StringBuffer(200);
			String charset = device.getCharset();
			catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			catalogXml.append("<Query>\r\n");
			catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
			catalogXml.append("<SN>" + sn + "</SN>\r\n");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			catalogXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), "z9hG4bK-ViaCatalog-" + tm, "FromCat" + tm, null, callIdHeader);

			transmitRequest(device, request, errorEvent);
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 查询录像信息
	 * 
	 * @param device 视频设备
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 */  
	@Override
	public boolean recordInfoQuery(Device device, String channelId, String startTime, String endTime, int sn, Integer secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) {
		if (secrecy == null) {
			secrecy = 0;
		}
		if (type == null) {
			type = "all";
		}
		try {
			StringBuffer recordInfoXml = new StringBuffer(200);
			String charset = device.getCharset();
			recordInfoXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			recordInfoXml.append("<Query>\r\n");
			recordInfoXml.append("<CmdType>RecordInfo</CmdType>\r\n");
			recordInfoXml.append("<SN>" + sn + "</SN>\r\n");
			recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			if (startTime != null) {
				recordInfoXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime) + "</StartTime>\r\n");
			}
			if (endTime != null) {
				recordInfoXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime) + "</EndTime>\r\n");
			}
			if (secrecy != null) {
				recordInfoXml.append("<Secrecy> "+ secrecy + " </Secrecy>\r\n");
			}
			if (type != null) {
				// 大华NVR要求必须增加一个值为all的文本元素节点Type
				recordInfoXml.append("<Type>" + type+"</Type>\r\n");
			}
			recordInfoXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, recordInfoXml.toString(),
					"z9hG4bK-ViaRecordInfo-" + tm, "fromRec" + tm, null, callIdHeader);

			transmitRequest(device, request, errorEvent, okEvent);
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

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
	@Override
	public boolean alarmInfoQuery(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType,
								 String startTime, String endTime, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Query>\r\n");
			cmdXml.append("<CmdType>Alarm</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			if (!StringUtils.isEmpty(startPriority)) {
				cmdXml.append("<StartAlarmPriority>" + startPriority + "</StartAlarmPriority>\r\n");
			}
			if (!StringUtils.isEmpty(endPriority)) {
				cmdXml.append("<EndAlarmPriority>" + endPriority + "</EndAlarmPriority>\r\n");
			}
			if (!StringUtils.isEmpty(alarmMethod)) {
				cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
			}
			if (!StringUtils.isEmpty(alarmType)) {
				cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
			}
			if (!StringUtils.isEmpty(startTime)) {
				cmdXml.append("<StartAlarmTime>" + startTime + "</StartAlarmTime>\r\n");
			}
			if (!StringUtils.isEmpty(endTime)) {
				cmdXml.append("<EndAlarmTime>" + endTime + "</EndAlarmTime>\r\n");
			}
			cmdXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromAlarm" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 查询设备配置
	 * 
	 * @param device 		视频设备
	 * @param channelId		通道编码（可选）
	 * @param configType	配置类型：
	 */
	@Override
	public boolean deviceConfigQuery(Device device, String channelId, String configType,  SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Query>\r\n");
			cmdXml.append("<CmdType>ConfigDownload</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			cmdXml.append("<ConfigType>" + configType + "</ConfigType>\r\n");
			cmdXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromConfig" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 查询设备预置位置
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean presetQuery(Device device, String channelId, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Query>\r\n");
			cmdXml.append("<CmdType>PresetQuery</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			cmdXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, "FromConfig" + tm, null, callIdHeader);
			transmitRequest(device, request, errorEvent);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		} 
	}

	/**
	 * 查询移动设备位置数据
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean mobilePostitionQuery(Device device, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer mobilePostitionXml = new StringBuffer(200);
			String charset = device.getCharset();
			mobilePostitionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			mobilePostitionXml.append("<Query>\r\n");
			mobilePostitionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
			mobilePostitionXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			mobilePostitionXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			mobilePostitionXml.append("<Interval>60</Interval>\r\n");
			mobilePostitionXml.append("</Query>\r\n");
			
			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createMessageRequest(device, mobilePostitionXml.toString(), "z9hG4bK-viaPos-" + tm, "fromTagPos" + tm, null, callIdHeader);

			transmitRequest(device, request, errorEvent);
			
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 订阅、取消订阅移动位置
	 * 
	 * @param device	视频设备
	 * @return			true = 命令发送成功
	 */
	@Override
	public boolean mobilePositionSubscribe(Device device, Dialog dialog, SipSubscribe.Event okEvent ,SipSubscribe.Event errorEvent) {
		try {
			StringBuffer subscribePostitionXml = new StringBuffer(200);
			String charset = device.getCharset();
			subscribePostitionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			subscribePostitionXml.append("<Query>\r\n");
			subscribePostitionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
			subscribePostitionXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			subscribePostitionXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			if (device.getSubscribeCycleForMobilePosition() > 0) {
				subscribePostitionXml.append("<Interval>" + String.valueOf(device.getMobilePositionSubmissionInterval()) + "</Interval>\r\n");
			}
			subscribePostitionXml.append("</Query>\r\n");

			Request request;
			if (dialog != null) {
				logger.info("发送移动位置订阅消息时 dialog的状态为： {}", dialog.getState());
				request = dialog.createRequest(Request.SUBSCRIBE);
				ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
				request.setContent(subscribePostitionXml.toString(), contentTypeHeader);
				ExpiresHeader expireHeader = sipFactory.createHeaderFactory().createExpiresHeader(device.getSubscribeCycleForMobilePosition());
				request.addHeader(expireHeader);
			}else {
				String tm = Long.toString(System.currentTimeMillis());
				CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
						: udpSipProvider.getNewCallId();
				request = headerProvider.createSubscribeRequest(device, subscribePostitionXml.toString(), "z9hG4bK-viaPos-" + tm, "fromTagPos" + tm, null, device.getSubscribeCycleForMobilePosition(), "presence" ,callIdHeader); //Position;id=" + tm.substring(tm.length() - 4));
			}
			transmitRequest(device, request, errorEvent, okEvent);

			return true;

		} catch ( NumberFormatException | ParseException | InvalidArgumentException	| SipException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 订阅、取消订阅报警信息
	 * 
	 * @param device		视频设备
	 * @param expires		订阅过期时间（0 = 取消订阅）
	 * @param startPriority	报警起始级别（可选）
	 * @param endPriority	报警终止级别（可选）
	 * @param alarmMethod	报警方式条件（可选）
	 * @param alarmType		报警类型
	 * @param startTime		报警发生起始时间（可选）
	 * @param endTime		报警发生终止时间（可选）
	 * @return				true = 命令发送成功
	 */
	@Override
	public boolean alarmSubscribe(Device device, int expires, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Query>\r\n");
			cmdXml.append("<CmdType>Alarm</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			if (!StringUtils.isEmpty(startPriority)) {
				cmdXml.append("<StartAlarmPriority>" + startPriority + "</StartAlarmPriority>\r\n");
			}
			if (!StringUtils.isEmpty(endPriority)) {
				cmdXml.append("<EndAlarmPriority>" + endPriority + "</EndAlarmPriority>\r\n");
			}
			if (!StringUtils.isEmpty(alarmMethod)) {
				cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
			}
			if (!StringUtils.isEmpty(alarmType)) {
				cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
			}
			if (!StringUtils.isEmpty(startTime)) {
				cmdXml.append("<StartAlarmTime>" + startTime + "</StartAlarmTime>\r\n");
			}
			if (!StringUtils.isEmpty(endTime)) {
				cmdXml.append("<EndAlarmTime>" + endTime + "</EndAlarmTime>\r\n");
			}
			cmdXml.append("</Query>\r\n");

			String tm = Long.toString(System.currentTimeMillis());

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();

			Request request = headerProvider.createSubscribeRequest(device, cmdXml.toString(), "z9hG4bK-viaPos-" + tm, "fromTagPos" + tm, null, expires, "presence" , callIdHeader);
			transmitRequest(device, request);

			return true;

		} catch ( NumberFormatException | ParseException | InvalidArgumentException	| SipException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean catalogSubscribe(Device device, Dialog dialog, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) {
		try {
			StringBuffer cmdXml = new StringBuffer(200);
			String charset = device.getCharset();
			cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			cmdXml.append("<Query>\r\n");
			cmdXml.append("<CmdType>Catalog</CmdType>\r\n");
			cmdXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			cmdXml.append("</Query>\r\n");


			Request request;
			if (dialog != null) {
				logger.info("发送目录订阅消息时 dialog的状态为： {}", dialog.getState());
				request = dialog.createRequest(Request.SUBSCRIBE);
				ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
				request.setContent(cmdXml.toString(), contentTypeHeader);
				ExpiresHeader expireHeader = sipFactory.createHeaderFactory().createExpiresHeader(device.getSubscribeCycleForMobilePosition());
				request.addHeader(expireHeader);
			}else {
				String tm = Long.toString(System.currentTimeMillis());

				CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
						: udpSipProvider.getNewCallId();

				// 有效时间默认为60秒以上
				request = headerProvider.createSubscribeRequest(device, cmdXml.toString(), "z9hG4bK-viaPos-" + tm,
						"fromTagPos" + tm, null, device.getSubscribeCycleForCatalog(), "Catalog" ,
						callIdHeader);

			}
			transmitRequest(device, request, errorEvent, okEvent);
			return true;

		} catch ( NumberFormatException | ParseException | InvalidArgumentException	| SipException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean dragZoomCmd(Device device, String channelId, String cmdString) {
		try {
			StringBuffer dragXml = new StringBuffer(200);
			String charset = device.getCharset();
			dragXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
			dragXml.append("<Control>\r\n");
			dragXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			dragXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
			if (StringUtils.isEmpty(channelId)) {
				dragXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			} else {
				dragXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			}
			dragXml.append(cmdString);
			dragXml.append("</Control>\r\n");
			String tm = Long.toString(System.currentTimeMillis());
			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();
			Request request = headerProvider.createMessageRequest(device, dragXml.toString(), "z9hG4bK-ViaPtz-" + tm, "FromPtz" + tm, null, callIdHeader);
			logger.debug("拉框信令： " + request.toString());
			transmitRequest(device, request);
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
		return false;
	}


	private ClientTransaction transmitRequest(Device device, Request request) throws SipException {
		return transmitRequest(device, request, null, null);
	}

	private ClientTransaction transmitRequest(Device device, Request request, SipSubscribe.Event errorEvent) throws SipException {
		return transmitRequest(device, request, errorEvent, null);
	}

	private ClientTransaction transmitRequest(Device device, Request request, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws SipException {
		ClientTransaction clientTransaction = null;
		if("TCP".equals(device.getTransport())) {
			clientTransaction = tcpSipProvider.getNewClientTransaction(request);
		} else if("UDP".equals(device.getTransport())) {
			clientTransaction = udpSipProvider.getNewClientTransaction(request);
		}
		if (request.getHeader(UserAgentHeader.NAME) == null) {
			List<String> agentParam = new ArrayList<>();
			agentParam.add("wvp-pro");
			// TODO 添加版本信息以及日期
			UserAgentHeader userAgentHeader = null;
			try {
				userAgentHeader = sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			request.addHeader(userAgentHeader);
		}
		CallIdHeader callIdHeader = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
		// 添加错误订阅
		if (errorEvent != null) {
			sipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), (eventResult -> {
				errorEvent.response(eventResult);
				sipSubscribe.removeErrorSubscribe(eventResult.callId);
			}));
		}
		// 添加订阅
		if (okEvent != null) {
			sipSubscribe.addOkSubscribe(callIdHeader.getCallId(), eventResult ->{
				okEvent.response(eventResult);
				sipSubscribe.removeOkSubscribe(eventResult.callId);
			});
		}

		clientTransaction.sendRequest();
		return clientTransaction;
	}

	/**
	 * 回放暂停
	 */
	@Override
	public void playPauseCmd(Device device, StreamInfo streamInfo) {
		try {
			Long cseq = redisCatchStorage.getCSEQ(Request.INFO);
			StringBuffer content = new StringBuffer(200);
			content.append("PAUSE RTSP/1.0\r\n");
			content.append("CSeq: " + cseq + "\r\n");
			content.append("PauseTime: now\r\n");
			Request request = headerProvider.createInfoRequest(device, streamInfo, content.toString());
			if (request == null) {
				return;
			}
			logger.info(request.toString());
			ClientTransaction clientTransaction = null;
			if ("TCP".equals(device.getTransport())) {
				clientTransaction = tcpSipProvider.getNewClientTransaction(request);
			} else if ("UDP".equals(device.getTransport())) {
				clientTransaction = udpSipProvider.getNewClientTransaction(request);
			}
			if (clientTransaction != null) {
				clientTransaction.sendRequest();
			}

		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 回放恢复
	 */
	@Override
	public void playResumeCmd(Device device, StreamInfo streamInfo) {
		try {
			Long cseq = redisCatchStorage.getCSEQ(Request.INFO);
			StringBuffer content = new StringBuffer(200);
			content.append("PLAY RTSP/1.0\r\n");
			content.append("CSeq: " + cseq + "\r\n");
			content.append("Range: npt=now-\r\n");
			Request request = headerProvider.createInfoRequest(device, streamInfo, content.toString());
			if (request == null) {
				return;
			}
			logger.info(request.toString());
			ClientTransaction clientTransaction = null;
			if ("TCP".equals(device.getTransport())) {
				clientTransaction = tcpSipProvider.getNewClientTransaction(request);
			} else if ("UDP".equals(device.getTransport())) {
				clientTransaction = udpSipProvider.getNewClientTransaction(request);
			}

			clientTransaction.sendRequest();

		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 回放拖动播放
	 */
	@Override
	public void playSeekCmd(Device device, StreamInfo streamInfo, long seekTime) {
		try {
			Long cseq = redisCatchStorage.getCSEQ(Request.INFO);
			StringBuffer content = new StringBuffer(200);
			content.append("PLAY RTSP/1.0\r\n");
			content.append("CSeq: " + cseq + "\r\n");
			content.append("Range: npt=" + Math.abs(seekTime) + "-\r\n");

			Request request = headerProvider.createInfoRequest(device, streamInfo, content.toString());
			if (request == null) {
				return;
			}
			logger.info(request.toString());
			ClientTransaction clientTransaction = null;
			if ("TCP".equals(device.getTransport())) {
				clientTransaction = tcpSipProvider.getNewClientTransaction(request);
			} else if ("UDP".equals(device.getTransport())) {
				clientTransaction = udpSipProvider.getNewClientTransaction(request);
			}

			clientTransaction.sendRequest();

		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 回放倍速播放
	 */
	@Override
	public void playSpeedCmd(Device device, StreamInfo streamInfo, Double speed) {
		try {
			Long cseq = redisCatchStorage.getCSEQ(Request.INFO);
			StringBuffer content = new StringBuffer(200);
			content.append("PLAY RTSP/1.0\r\n");
			content.append("CSeq: " + cseq + "\r\n");
			content.append("Scale: " + String.format("%.1f",speed) + "\r\n");
			Request request = headerProvider.createInfoRequest(device, streamInfo, content.toString());
			if (request == null) {
				return;
			}
			logger.info(request.toString());
			ClientTransaction clientTransaction = null;
			if ("TCP".equals(device.getTransport())) {
				clientTransaction = tcpSipProvider.getNewClientTransaction(request);
			} else if ("UDP".equals(device.getTransport())) {
				clientTransaction = udpSipProvider.getNewClientTransaction(request);
			}

			clientTransaction.sendRequest();

		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean sendAlarmMessage(Device device, DeviceAlarm deviceAlarm) {
		if (device == null) {
			return false;
		}
		logger.info("[发送 报警通知] {}/{}->{},{}", device.getDeviceId(), deviceAlarm.getChannelId(),
				deviceAlarm.getLongitude(), deviceAlarm.getLatitude());
		try {
			String characterSet = device.getCharset();
			StringBuffer deviceStatusXml = new StringBuffer(600);
			deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
			deviceStatusXml.append("<Notify>\r\n");
			deviceStatusXml.append("<CmdType>Alarm</CmdType>\r\n");
			deviceStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			deviceStatusXml.append("<DeviceID>" + deviceAlarm.getChannelId() + "</DeviceID>\r\n");
			deviceStatusXml.append("<AlarmPriority>" + deviceAlarm.getAlarmPriority() + "</AlarmPriority>\r\n");
			deviceStatusXml.append("<AlarmMethod>" + deviceAlarm.getAlarmMethod() + "</AlarmMethod>\r\n");
			deviceStatusXml.append("<AlarmTime>" + deviceAlarm.getAlarmTime() + "</AlarmTime>\r\n");
			deviceStatusXml.append("<AlarmDescription>" + deviceAlarm.getAlarmDescription() + "</AlarmDescription>\r\n");
			deviceStatusXml.append("<Longitude>" + deviceAlarm.getLongitude() + "</Longitude>\r\n");
			deviceStatusXml.append("<Latitude>" + deviceAlarm.getLatitude() + "</Latitude>\r\n");
			deviceStatusXml.append("<info>\r\n");
			deviceStatusXml.append("<AlarmType>" + deviceAlarm.getAlarmType() + "</AlarmType>\r\n");
			deviceStatusXml.append("</info>\r\n");
			deviceStatusXml.append("</Notify>\r\n");

			CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
					: udpSipProvider.getNewCallId();
			String tm = Long.toString(System.currentTimeMillis());
			Request request = headerProvider.createMessageRequest(device, deviceStatusXml.toString(), "z9hG4bK-ViaPtz-" + tm, "FromPtz" + tm, null, callIdHeader);
			transmitRequest(device, request);


		} catch (SipException | ParseException  e) {
			e.printStackTrace();
			return false;
		} catch (InvalidArgumentException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	private void sendNotify(Device device, String catalogXmlContent,
							SubscribeInfo subscribeInfo, SipSubscribe.Event errorEvent,  SipSubscribe.Event okEvent )
			throws NoSuchFieldException, IllegalAccessException, SipException, ParseException {
		MessageFactoryImpl messageFactory = (MessageFactoryImpl) sipFactory.createMessageFactory();
		String characterSet = device.getCharset();
		// 设置编码， 防止中文乱码
		messageFactory.setDefaultContentEncodingCharset(characterSet);
		Dialog dialog  = subscribeInfo.getDialog();
		if (dialog == null || !dialog.getState().equals(DialogState.CONFIRMED)) {
			return;
		}
		SIPRequest notifyRequest = (SIPRequest)dialog.createRequest(Request.NOTIFY);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		notifyRequest.setContent(catalogXmlContent, contentTypeHeader);

		SubscriptionStateHeader subscriptionState = sipFactory.createHeaderFactory()
				.createSubscriptionStateHeader(SubscriptionStateHeader.ACTIVE);
		notifyRequest.addHeader(subscriptionState);

		EventHeader event = sipFactory.createHeaderFactory().createEventHeader(subscribeInfo.getEventType());
		if (subscribeInfo.getEventId() != null) {
			event.setEventId(subscribeInfo.getEventId());
		}
		notifyRequest.addHeader(event);

		SipURI sipURI = (SipURI) notifyRequest.getRequestURI();
		if (subscribeInfo.getTransaction() != null) {
			SIPRequest request = (SIPRequest) subscribeInfo.getTransaction().getRequest();
			sipURI.setHost(request.getRemoteAddress().getHostAddress());
			sipURI.setPort(request.getRemotePort());
		}else {
			sipURI.setHost(device.getIp());
			sipURI.setPort(device.getPort());
		}

		ClientTransaction transaction = null;
		if ("TCP".equals(device.getTransport())) {
			transaction = tcpSipProvider.getNewClientTransaction(notifyRequest);
		} else if ("UDP".equals(device.getTransport())) {
			transaction = udpSipProvider.getNewClientTransaction(notifyRequest);
		}
		// 添加错误订阅
		if (errorEvent != null) {
			sipSubscribe.addErrorSubscribe(subscribeInfo.getCallId(), errorEvent);
		}
		// 添加订阅
		if (okEvent != null) {
			sipSubscribe.addOkSubscribe(subscribeInfo.getCallId(), okEvent);
		}
		if (transaction == null) {
			logger.error("平台{}的Transport错误：{}",device.getDeviceId(), device.getTransport());
			return;
		}
		dialog.sendRequest(transaction);

	}
}
