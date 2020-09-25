package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.springframework.util.StringUtils;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.gb28181.auth.RegisterLogicHandler;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Host;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Expires;

/**    
 * @Description:收到注册请求 处理 
 * @author: swwheihei
 * @date:   2020年5月3日 下午4:47:25     
 */
public class RegisterRequestProcessor extends SIPRequestAbstractProcessor {

	private SipConfig sipConfig;
	
	private RegisterLogicHandler handler;
	
	private IVideoManagerStorager storager;
	
	private EventPublisher publisher;
	
	/***
	 * 收到注册请求 处理
	 * 
	 * @param request
	 *            请求消息
	 */ 
	@Override
	public void process(RequestEvent evt) {
		try {
			System.out.println("收到注册请求，开始处理");
			Request request = evt.getRequest();

			Response response = null; 
			boolean passwordCorrect = false;
			// 注册标志  0：未携带授权头或者密码错误  1：注册成功   2：注销成功
			int registerFlag = 0;
			Device device = null;
			AuthorizationHeader authorhead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME); 
			// 校验密码是否正确
			if (authorhead != null) {
				passwordCorrect = new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request,
						sipConfig.getSipPassword());
			}

			// 未携带授权头或者密码错误 回复401
			if (authorhead == null || !passwordCorrect) {
				
				if (authorhead == null) {
					System.out.println("未携带授权头 回复401");
				} else if (!passwordCorrect) {
					System.out.println("密码错误 回复401");
				}
				response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
				new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getSipDomain());
			}
			// 携带授权头并且密码正确
			else if (passwordCorrect) {
				response = getMessageFactory().createResponse(Response.OK, request);
				// 添加date头
				response.addHeader(getHeaderFactory().createDateHeader(Calendar.getInstance(Locale.ENGLISH)));
				ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(Expires.NAME);
				// 添加Contact头
				response.addHeader(request.getHeader(ContactHeader.NAME));
				// 添加Expires头
				response.addHeader(request.getExpires());
				
				// 1.获取到通信地址等信息，保存到Redis
				FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
				ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
				String received = viaHeader.getReceived();
				int rPort = viaHeader.getRPort();
				// 本地模拟设备 received 为空 rPort 为 -1
				// 解析本地地址替代
				if (StringUtils.isEmpty(received) || rPort == -1) {
					received = viaHeader.getHost();
					rPort = viaHeader.getPort();
				}
				//
				Host host = new Host();
				host.setIp(received);
				host.setPort(rPort);
				host.setAddress(received.concat(":").concat(String.valueOf(rPort)));
				AddressImpl address = (AddressImpl) fromHeader.getAddress();
				SipUri uri = (SipUri) address.getURI();
				String deviceId = uri.getUser();
				device = new Device();
				device.setDeviceId(deviceId);
				device.setHost(host);
				// 注销成功
				if (expiresHeader != null && expiresHeader.getExpires() == 0) {
					registerFlag = 2;
				}
				// 注册成功
				else {
					registerFlag = 1;
					// 判断TCP还是UDP
					boolean isTcp = false;
					ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
					String transport = reqViaHeader.getTransport();
					if (transport.equals("TCP")) {
						isTcp = true;
					}
					device.setTransport(isTcp ? "TCP" : "UDP");
				}
			}
			getServerTransaction(evt).sendResponse(response);
			// 注册成功
			// 保存到redis
			// 下发catelog查询目录
			if (registerFlag == 1 && device != null) {
				System.out.println("注册成功! deviceId:" + device.getDeviceId());
				storager.updateDevice(device);
				publisher.onlineEventPublish(device.getDeviceId(), VideoManagerConstants.EVENT_ONLINE_REGISTER);
				handler.onRegister(device);
			} else if (registerFlag == 2) {
				System.out.println("注销成功! deviceId:" + device.getDeviceId());
				publisher.outlineEventPublish(device.getDeviceId(), VideoManagerConstants.EVENT_OUTLINE_UNREGISTER);
			}
		} catch (SipException | InvalidArgumentException | NoSuchAlgorithmException | ParseException e) {
			e.printStackTrace();
		}
		
	}
	
	public void setSipConfig(SipConfig sipConfig) {
		this.sipConfig = sipConfig;
	}

	public void setHandler(RegisterLogicHandler handler) {
		this.handler = handler;
	}

	public void setVideoManagerStorager(IVideoManagerStorager storager) {
		this.storager = storager;
	}

	public void setPublisher(EventPublisher publisher) {
		this.publisher = publisher;
	}

}
