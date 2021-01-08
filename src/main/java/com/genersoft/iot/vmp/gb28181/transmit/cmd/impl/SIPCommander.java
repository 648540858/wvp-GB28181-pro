package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import java.text.ParseException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.Header;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;

/**    
 * @Description:设备能力接口，用于定义设备的控制、查询能力   
 * @author: swwheihei
 * @date:   2020年5月3日 下午9:22:48     
 */
@Component
public class SIPCommander implements ISIPCommander {

	private final Logger logger = LoggerFactory.getLogger(SIPCommander.class);
	
	@Autowired
	private SipConfig sipConfig;
	
	@Autowired
	private SIPRequestHeaderProvider headerProvider;
	
	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;
	
	@Autowired
	@Qualifier(value="tcpSipProvider")
	private SipProvider tcpSipProvider;
	
	@Autowired
	@Qualifier(value="udpSipProvider")
	private SipProvider udpSipProvider;

	@Autowired
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Value("${media.rtp.enable}")
	private boolean rtpEnable;

	@Value("${media.seniorSdp}")
	private boolean seniorSdp;

	@Autowired
	private ZLMHttpHookSubscribe subscribe;

	@Autowired
	private SipSubscribe sipSubscribe;



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
		return ptzCmd(device, channelId, leftRight, upDown, 0, sipConfig.getSpeed(), 0);
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
		return ptzCmd(device, channelId, 0, 0, inOut, 0, sipConfig.getSpeed());
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
	* @param cmdCode		指令码
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
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度
     * @param zoomSpeed  镜头缩放速度
	 */
	@Override
	public boolean ptzCmd(Device device, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
			int zoomSpeed) {
		try {
			String cmdStr= cmdString(leftRight, upDown, inOut, moveSpeed, zoomSpeed);
			StringBuffer ptzXml = new StringBuffer(200);
			ptzXml.append("<?xml version=\"1.0\" ?>\r\n");
			ptzXml.append("<Control>\r\n");
			ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			ptzXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
			ptzXml.append("<Info>\r\n");
			ptzXml.append("</Info>\r\n");
			ptzXml.append("</Control>\r\n");
			
			Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), "ViaPtzBranch", "FromPtzTag", null);
			
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
			System.out.println("控制字符串：" + cmdStr);
			StringBuffer ptzXml = new StringBuffer(200);
			ptzXml.append("<?xml version=\"1.0\" ?>\r\n");
			ptzXml.append("<Control>\r\n");
			ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
			ptzXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
			ptzXml.append("<Info>\r\n");
			ptzXml.append("</Info>\r\n");
			ptzXml.append("</Control>\r\n");
			
			Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), "ViaPtzBranch", "FromPtzTag", null);
			transmitRequest(device, request);
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
	public void playStreamCmd(Device device, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent) {
		try {

			String ssrc = streamSession.createPlaySsrc();
			String streamId = null;
			if (rtpEnable) {
				streamId = String.format("gb_play_%s_%s", device.getDeviceId(), channelId);
			}else {
				streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
			}
			String streamMode = device.getStreamMode().toUpperCase();
			MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
			if (mediaInfo == null) {
				logger.warn("点播时发现ZLM尚未连接...");
				return;
			}
			String mediaPort = null;
			// 使用动态udp端口
			if (rtpEnable) {
				mediaPort = zlmrtpServerFactory.createRTPServer(streamId) + "";
			}else {
				mediaPort = mediaInfo.getRtpProxyPort();
			}

			// 添加订阅
			JSONObject subscribeKey = new JSONObject();
			subscribeKey.put("app", "rtp");
			subscribeKey.put("id", streamId);

			subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_publish, subscribeKey, event);
			//
			StringBuffer content = new StringBuffer(200);
			content.append("v=0\r\n");
//			content.append("o="+channelId+" 0 0 IN IP4 "+mediaInfo.getWanIp()+"\r\n");
			content.append("o="+"00000"+" 0 0 IN IP4 "+mediaInfo.getWanIp()+"\r\n");
			content.append("s=Play\r\n");
			content.append("c=IN IP4 "+mediaInfo.getWanIp()+"\r\n");
			content.append("t=0 0\r\n");

			if (seniorSdp) {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 126 125 99 34 98 97 96\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 126 125 99 34 98 97 96\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" RTP/AVP 126 125 99 34 98 97 96\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:126 H264/90000\r\n");
				content.append("a=rtpmap:125 H264S/90000\r\n");
				content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
				content.append("a=fmtp:99 profile-level-id=3\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}else {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 96 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 96 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" RTP/AVP 96 98 97\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=recvonly\r\n");
					content.append("a=rtpmap:96 PS/90000\r\n");
					content.append("a=rtpmap:98 H264/90000\r\n");
					content.append("a=rtpmap:97 MPEG4/90000\r\n");
					if ("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
						content.append("a=setup:passive\r\n");
						content.append("a=connection:new\r\n");
					} else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					} else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
						content.append("a=setup:active\r\n");
						content.append("a=connection:new\r\n");
					}
				}
			}

			content.append("y="+ssrc+"\r\n");//ssrc

//			String fromTag = UUID.randomUUID().toString();
//			Request request = headerProvider.createInviteRequest(device, channelId, content.toString(), null, fromTag, null, ssrc);

			Request request = headerProvider.createInviteRequest(device, channelId, content.toString(), null, "live", null, ssrc);

			ClientTransaction transaction = transmitRequest(device, request, errorEvent);
			streamSession.put(streamId, transaction);



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
	public void playbackStreamCmd(Device device, String channelId, String startTime, String endTime, ZLMHttpHookSubscribe.Event event
			, SipSubscribe.Event errorEvent) {
		try {
			MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
			String ssrc = streamSession.createPlayBackSsrc();
			String streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
			// 添加订阅
			JSONObject subscribeKey = new JSONObject();
			subscribeKey.put("app", "rtp");
			subscribeKey.put("id", streamId);

			subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_publish, subscribeKey, event);

			//
			StringBuffer content = new StringBuffer(200);
	        content.append("v=0\r\n");
	        content.append("o="+sipConfig.getSipId()+" 0 0 IN IP4 "+sipConfig.getSipIp()+"\r\n");
	        content.append("s=Playback\r\n");
	        content.append("u="+channelId+":0\r\n");
	        content.append("c=IN IP4 "+mediaInfo.getWanIp()+"\r\n");
	        content.append("t="+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime)+" "
					+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) +"\r\n");
			String mediaPort = null;
			// 使用动态udp端口
			if (rtpEnable) {
				mediaPort = zlmrtpServerFactory.createRTPServer(streamId) + "";
			}else {
				mediaPort = mediaInfo.getRtpProxyPort();
			}
			String streamMode = device.getStreamMode().toUpperCase();

			if (seniorSdp) {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 126 125 99 34 98 97 96\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 126 125 99 34 98 97 96\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" RTP/AVP 126 125 99 34 98 97 96\r\n");
				}
				content.append("a=recvonly\r\n");
				content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:126 H264/90000\r\n");
				content.append("a=rtpmap:125 H264S/90000\r\n");
				content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
				content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
				content.append("a=fmtp:99 profile-level-id=3\r\n");
				content.append("a=rtpmap:98 H264/90000\r\n");
				content.append("a=rtpmap:97 MPEG4/90000\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				if("TCP-PASSIVE".equals(streamMode)){ // tcp被动模式
					content.append("a=setup:passive\r\n");
					content.append("a=connection:new\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
					content.append("a=setup:active\r\n");
					content.append("a=connection:new\r\n");
				}
			}else {
				if("TCP-PASSIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 96 98 97\r\n");
				}else if ("TCP-ACTIVE".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" TCP/RTP/AVP 96 98 97\r\n");
				}else if("UDP".equals(streamMode)) {
					content.append("m=video "+ mediaPort +" RTP/AVP 96 98 97\r\n");
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

	        content.append("y="+ssrc+"\r\n");//ssrc
	        
	        Request request = headerProvider.createPlaybackInviteRequest(device, channelId, content.toString(), null, "playback", null);

	        ClientTransaction transaction = transmitRequest(device, request, errorEvent);
	        streamSession.put(streamId, transaction);

		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}



	/**
	 * 视频流停止
	 * 
	 */
	@Override
	public void streamByeCmd(String ssrc) {
		streamByeCmd(ssrc, null);
	}
	@Override
	public void streamByeCmd(String streamId, SipSubscribe.Event okEvent) {
		
		try {
			ClientTransaction transaction = streamSession.get(streamId);
			// 服务重启后
			if (transaction == null) {
				StreamInfo streamInfo = redisCatchStorage.queryPlayByStreamId(streamId);
				if (streamInfo != null) {

				}
				return;
			}
			
			Dialog dialog = transaction.getDialog();
			if (dialog == null) {
				return;
			}



			Request byeRequest = dialog.createRequest(Request.BYE);
			SipURI byeURI = (SipURI) byeRequest.getRequestURI();
			String vh = transaction.getRequest().getHeader(ViaHeader.NAME).toString();
			Pattern p = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\:(\\d+)");
			Matcher matcher = p.matcher(vh);
			if (matcher.find()) {
				byeURI.setHost(matcher.group(1));
			}
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

			streamSession.remove(streamId);
			zlmrtpServerFactory.closeRTPServer(streamId);
		} catch (TransactionDoesNotExistException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (ParseException e) {
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
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 音视频录像控制
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */  
	@Override
	public boolean recordCmd(Device device, String channelId) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 报警布防/撤防命令
	 * 
	 * @param device  视频设备
	 */  
	@Override
	public boolean guardCmd(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 报警复位命令
	 * 
	 * @param device  视频设备
	 */  
	@Override
	public boolean alarmCmd(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */ 
	@Override
	public boolean iFameCmd(Device device, String channelId) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 看守位控制命令
	 * 
	 * @param device  视频设备
	 */  
	@Override
	public boolean homePositionCmd(Device device) {
		// TODO Auto-generated method stub
		return false;
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
	 * 查询设备状态
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean deviceStatusQuery(Device device) {
		// TODO Auto-generated method stub
		return false;
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
			catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
			catalogXml.append("<Query>\r\n");
			catalogXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
			catalogXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			catalogXml.append("</Query>\r\n");
			
			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), "ViaDeviceInfoBranch", "FromDeviceInfoTag", null);

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
	public boolean catalogQuery(Device device, SipSubscribe.Event errorEvent) {
		// 清空通道
		storager.cleanChannelsForDevice(device.getDeviceId());
		try {
			StringBuffer catalogXml = new StringBuffer(200);
			catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
			catalogXml.append("<Query>\r\n");
			catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
			catalogXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
			catalogXml.append("</Query>\r\n");
			
			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), "ViaCatalogBranch", "FromCatalogTag", null);

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
	public boolean recordInfoQuery(Device device, String channelId, String startTime, String endTime) {
		
		try {
			StringBuffer recordInfoXml = new StringBuffer(200);
			recordInfoXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
			recordInfoXml.append("<Query>\r\n");
			recordInfoXml.append("<CmdType>RecordInfo</CmdType>\r\n");
			recordInfoXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
			recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
			recordInfoXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime) + "</StartTime>\r\n");
			recordInfoXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime) + "</EndTime>\r\n");
			recordInfoXml.append("<Secrecy>0</Secrecy>\r\n");
			// 大华NVR要求必须增加一个值为all的文本元素节点Type
			recordInfoXml.append("<Type>all</Type>\r\n");
			recordInfoXml.append("</Query>\r\n");
			
			Request request = headerProvider.createMessageRequest(device, recordInfoXml.toString(), "ViaRecordInfoBranch", "FromRecordInfoTag", null);

			transmitRequest(device, request);
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 查询报警信息
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean alarmInfoQuery(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 查询设备配置
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean configQuery(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 查询设备预置位置
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean presetQuery(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 查询移动设备位置数据
	 * 
	 * @param device 视频设备
	 */  
	@Override
	public boolean mobilePostitionQuery(Device device) {
		// TODO Auto-generated method stub
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

		CallIdHeader callIdHeader = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
		// 添加错误订阅
		if (errorEvent != null) {
			sipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), errorEvent);
		}
		// 添加订阅
		if (okEvent != null) {
			sipSubscribe.addOkSubscribe(callIdHeader.getCallId(), okEvent);
		}

		clientTransaction.sendRequest();
		return clientTransaction;
	}




	@Override
	public void closeRTPServer(Device device, String channelId) {
		if (rtpEnable) {
			String streamId = String.format("gb_play_%s_%s", device.getDeviceId(), channelId);
			zlmrtpServerFactory.closeRTPServer(streamId);
		}
	}
}
