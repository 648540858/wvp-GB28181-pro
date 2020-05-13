package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import java.text.ParseException;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.gb28181.utils.SsrcUtil;

/**    
 * @Description:设备能力接口，用于定义设备的控制、查询能力   
 * @author: songww
 * @date:   2020年5月3日 下午9:22:48     
 */
@Component
public class SIPCommander implements ISIPCommander {
	
	@Autowired
	private SipConfig sipConfig;
	
	@Autowired
	private SIPRequestHeaderProvider headerProvider;
	
	@Autowired
	private SipLayer sipLayer;
	
	/**
	 * 云台方向放控制，使用配置文件中的默认镜头移动速度
	 * 
	 * @param device  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
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
			StringBuffer ptzXml = new StringBuffer(200);
			ptzXml.append("<?xml version=\"1.0\" ?>");
			ptzXml.append("<Control>");
			ptzXml.append("<CmdType>DeviceControl</CmdType>");
			ptzXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>");
			ptzXml.append("<DeviceID>" + channelId + "</DeviceID>");
			ptzXml.append("<PTZCmd>" + PtzCmdHelper.create(leftRight, upDown, inOut, moveSpeed, zoomSpeed) + "</PTZCmd>");
			ptzXml.append("<Info>");
			ptzXml.append("</Info>");
			ptzXml.append("</Control>");
			
			Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), "ViaPtzBranch", "FromPtzTag", "ToPtzTag");
			
			transmitRequest(device, request);
			
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
		return false;
	}

	/**
	 * 请求预览视频流
	 * 
	 * @param device  视频设备
	 * @param channelId  预览通道
	 */  
	@Override
	public String playStreamCmd(Device device, String channelId) {
		try {
			
			String ssrc = SsrcUtil.getPlaySsrc();
			//
			StringBuffer content = new StringBuffer(200);
	        content.append("v=0\r\n");
	        content.append("o="+channelId+" 0 0 IN IP4 "+sipConfig.getSipIp()+"\r\n");
	        content.append("s=Play\r\n");
	        content.append("c=IN IP4 "+sipConfig.getMediaIp()+"\r\n");
	        content.append("t=0 0\r\n");
	        if(device.getTransport().equals("TCP")) {
	        	content.append("m=video "+sipConfig.getMediaPort()+" TCP/RTP/AVP 96 98 97\r\n");
			}
	        if(device.getTransport().equals("UDP")) {
	        	content.append("m=video "+sipConfig.getMediaPort()+" RTP/AVP 96 98 97\r\n");
			}
	        content.append("a=sendrecv\r\n");
	        content.append("a=rtpmap:96 PS/90000\r\n");
	        content.append("a=rtpmap:98 H264/90000\r\n");
	        content.append("a=rtpmap:97 MPEG4/90000\r\n");
	        if(device.getTransport().equals("TCP")){
	             content.append("a=setup:passive\r\n");
	             content.append("a=connection:new\r\n");
	        }
	        content.append("y="+ssrc+"\r\n");//ssrc
	        
	        Request request = headerProvider.createInviteRequest(device, content.toString(), null, "live", null);
	
	        transmitRequest(device, request);
			return ssrc;
		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return null;
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
	public String playbackStreamCmd(Device device, String channelId, String startTime, String endTime) {
		try {
			
			String ssrc = SsrcUtil.getPlayBackSsrc();
			//
			StringBuffer content = new StringBuffer(200);
	        content.append("v=0\r\n");
	        content.append("o="+device.getDeviceId()+" 0 0 IN IP4 "+sipConfig.getSipIp()+"\r\n");
	        content.append("s=Playback\r\n");
	        content.append("u="+channelId+":3\r\n");
	        content.append("c=IN IP4 "+sipConfig.getMediaIp()+"\r\n");
	        content.append("t="+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime)+" "+DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) +"\r\n");
	        if(device.getTransport().equals("TCP")) {
	        	content.append("m=video "+sipConfig.getMediaPort()+" TCP/RTP/AVP 96 98 97\r\n");
			}
	        if(device.getTransport().equals("UDP")) {
	        	content.append("m=video "+sipConfig.getMediaPort()+" RTP/AVP 96 98 97\r\n");
			}
	        content.append("a=recvonly\r\n");
	        content.append("a=rtpmap:96 PS/90000\r\n");
	        content.append("a=rtpmap:98 H264/90000\r\n");
	        content.append("a=rtpmap:97 MPEG4/90000\r\n");
	        if(device.getTransport().equals("TCP")){
	             content.append("a=setup:passive\r\n");
	             content.append("a=connection:new\r\n");
	        }
	        content.append("y="+ssrc+"\r\n");//ssrc
	        
	        Request request = headerProvider.createInviteRequest(device, content.toString(), null, "live", null);
	
	        transmitRequest(device, request);
			return ssrc;
		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return null;
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
			catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>");
			catalogXml.append("<Query>");
			catalogXml.append("<CmdType>DeviceInfo</CmdType>");
			catalogXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>");
			catalogXml.append("</Query>");
			
			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), "ViaDeviceInfoBranch", "FromDeviceInfoTag", "ToDeviceInfoTag");
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
	public boolean catalogQuery(Device device) {
		try {
			StringBuffer catalogXml = new StringBuffer(200);
			catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>");
			catalogXml.append("<Query>");
			catalogXml.append("<CmdType>Catalog</CmdType>");
			catalogXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>");
			catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>");
			catalogXml.append("</Query>");
			
			Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), "ViaCatalogBranch", "FromCatalogTag", "ToCatalogTag");
			transmitRequest(device, request);
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
			recordInfoXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>");
			recordInfoXml.append("<Query>");
			recordInfoXml.append("<CmdType>RecordInfo</CmdType>");
			recordInfoXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>");
			recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>");
			recordInfoXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime) + "</StartTime>");
			recordInfoXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime) + "</EndTime>");
			recordInfoXml.append("<Secrecy>0</Secrecy>");
			// 大华NVR要求必须增加一个值为all的文本元素节点Type
			recordInfoXml.append("<Type>all</Type>");
			recordInfoXml.append("</Query>");
			
			Request request = headerProvider.createMessageRequest(device, recordInfoXml.toString(), "ViaRecordInfoBranch", "FromRecordInfoTag", "ToRecordInfoTag");
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
	
	private void transmitRequest(Device device, Request request) throws SipException {
		ClientTransaction clientTransaction = null;
		if(device.getTransport().equals("TCP")) {
			clientTransaction = sipLayer.getTcpSipProvider().getNewClientTransaction(request);
			//sipLayer.getTcpSipProvider().sendRequest(request);
		} else if(device.getTransport().equals("UDP")) {
			clientTransaction = sipLayer.getUdpSipProvider().getNewClientTransaction(request);
			//sipLayer.getUdpSipProvider().sendRequest(request);
		}
		clientTransaction.sendRequest();
	}

}
