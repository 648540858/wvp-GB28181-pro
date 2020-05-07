package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import java.text.ParseException;
import java.util.Random;

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
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

/**    
 * @Description:设备能力接口，用于定义设备的控制、查询能力   
 * @author: songww
 * @date:   2020年5月3日 下午9:22:48     
 */
@Component
public class SIPCommander implements ISIPCommander {
	
	@Autowired
	private SipConfig config;
	
	@Autowired
	private SIPRequestHeaderProvider headerProvider;
	
	@Autowired
	private SipLayer sipLayer;
	
	@Autowired
	private IVideoManagerStorager storager;
	 
	/**
	 * 云台方向放控制，使用配置文件中的默认镜头移动速度
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
	 */
	@Override
	public boolean ptzdirectCmd(String deviceId, String channelId, int leftRight, int upDown) {
		return ptzCmd(deviceId, channelId, leftRight, upDown, 0, config.getSpeed(), 0);
	}

	/**
	 * 云台方向放控制
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed  镜头移动速度
	 */
	@Override
	public boolean ptzdirectCmd(String deviceId, String channelId, int leftRight, int upDown, int moveSpeed) {
		return ptzCmd(deviceId, channelId, leftRight, upDown, 0, moveSpeed, 0);
	}

	/**
	 * 云台缩放控制，使用配置文件中的默认镜头缩放速度
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
	 */  
	@Override
	public boolean ptzZoomCmd(String deviceId, String channelId, int inOut) {
		return ptzCmd(deviceId, channelId, 0, 0, inOut, 0, config.getSpeed());
	}

	/**
	 * 云台缩放控制
	 * 
	 * @param deviceId  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param zoomSpeed  镜头缩放速度
	 */ 
	@Override
	public boolean ptzZoomCmd(String deviceId, String channelId, int inOut, int zoomSpeed) {
		return ptzCmd(deviceId, channelId, 0, 0, inOut, 0, zoomSpeed);
	}
  
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
	@Override
	public boolean ptzCmd(String deviceId, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
			int zoomSpeed) {
		try {
			Device device = storager.queryVideoDevice(deviceId);
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
			
			transmitRequest(device.getTransport(), request);
			
			return true;
		} catch (SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
		} 
		return false;
	}

	/**
	 * 请求预览视频流
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */  
	@Override
	public String playStreamCmd(String deviceId, String channelId) {
		try {
			
			Device device = storager.queryVideoDevice(deviceId);
			
			//生成ssrc标识数据流 10位数字
			String ssrc = "";
			Random random = new Random();
			// ZLMediaServer最大识别7FFFFFFF即2147483647，所以随机数不能超过这个数
			ssrc = String.valueOf(random.nextInt(2147483647));
			//
			StringBuffer content = new StringBuffer(200);
	        content.append("v=0\r\n");
	        content.append("o="+channelId+" 0 0 IN IP4 "+config.getSipIp()+"\r\n");
	        content.append("s=Play\r\n");
	        content.append("c=IN IP4 "+config.getMediaIp()+"\r\n");
	        content.append("t=0 0\r\n");
	        if(device.getTransport().equals("TCP")) {
	        	content.append("m=video "+config.getMediaPort()+" TCP/RTP/AVP 96 98 97\r\n");
			}
	        if(device.getTransport().equals("UDP")) {
	        	content.append("m=video "+config.getMediaPort()+" RTP/AVP 96 98 97\r\n");
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
	
	        transmitRequest(device.getTransport(), request);
			return ssrc;
		} catch ( SipException | ParseException | InvalidArgumentException e) {
			e.printStackTrace();
			return null;
		} 
	}

	/**
	 * 语音广播
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */
	@Override
	public String audioBroadcastCmd(String deviceId, String channelId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 音视频录像控制
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */  
	@Override
	public String recordCmd(String deviceId, String channelId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 报警布防/撤防命令
	 * 
	 * @param deviceId  视频设备
	 */  
	@Override
	public String guardCmd(String deviceId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 报警复位命令
	 * 
	 * @param deviceId  视频设备
	 */  
	@Override
	public String alarmCmd(String deviceId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 * 
	 * @param deviceId  视频设备
	 * @param channelId  预览通道
	 */ 
	@Override
	public String iFameCmd(String deviceId, String channelId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 看守位控制命令
	 * 
	 * @param deviceId  视频设备
	 */  
	@Override
	public String homePositionCmd(String deviceId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 设备配置命令
	 * 
	 * @param deviceId  视频设备
	 */  
	@Override
	public String deviceConfigCmd(String deviceId) {
		// TODO Auto-generated method stub
		return null;
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
			
			transmitRequest(device.getTransport(), request);
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
			
			transmitRequest(device.getTransport(), request);

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
	 */  
	@Override
	public boolean recordInfoQuery(Device device) {
		// TODO Auto-generated method stub
		return false;
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
	
	private void transmitRequest(String transport, Request request) throws SipException {
		if(transport.equals("TCP")) {
			sipLayer.getTcpSipProvider().sendRequest(request);
		} else if(transport.equals("UDP")) {
			sipLayer.getUdpSipProvider().sendRequest(request);
		}
	}
}
