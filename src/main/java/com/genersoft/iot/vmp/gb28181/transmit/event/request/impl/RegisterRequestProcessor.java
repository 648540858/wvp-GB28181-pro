package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.gb28181.auth.RegisterLogicHandler;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.WvpSipDate;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Expires;
import gov.nist.javax.sip.header.SIPDateHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

/**
 * SIP命令类型： REGISTER请求
 */
@Component
public class RegisterRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private Logger logger = LoggerFactory.getLogger(RegisterRequestProcessor.class);

	public String method = "REGISTER";

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private RegisterLogicHandler handler;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * 收到注册请求 处理
 	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		try {
			RequestEventExt evtExt = (RequestEventExt)evt;
			String requestAddress = evtExt.getRemoteIpAddress() + ":" + evtExt.getRemotePort();
			logger.info("[{}] 收到注册请求，开始处理", requestAddress);
			Request request = evt.getRequest();

			Response response = null; 
			boolean passwordCorrect = false;
			// 注册标志  0：未携带授权头或者密码错误  1：注册成功   2：注销成功
			int registerFlag = 0;
			FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
			AddressImpl address = (AddressImpl) fromHeader.getAddress();
			SipUri uri = (SipUri) address.getURI();
			String deviceId = uri.getUser();
			Device deviceInRedis = redisCatchStorage.getDevice(deviceId);
			Device device = storager.queryVideoDevice(deviceId);
			if (deviceInRedis != null && device == null) {
				// redis 存在脏数据
				redisCatchStorage.clearCatchByDeviceId(deviceId);
			}
			AuthorizationHeader authorhead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
			// 校验密码是否正确
			if (authorhead != null) {
				passwordCorrect = new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request,
						sipConfig.getPassword());
			}
			if (StringUtils.isEmpty(sipConfig.getPassword())){
				passwordCorrect = true;
			}

			// 未携带授权头或者密码错误 回复401
			if (authorhead == null ) {

				logger.info("[{}] 未携带授权头 回复401", requestAddress);
				response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
				new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
			}else {
				if (!passwordCorrect){
					// 注册失败
					response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
					response.setReasonPhrase("wrong password");
					logger.info("[{}] 密码/SIP服务器ID错误, 回复403", requestAddress);
				}else {
					// 携带授权头并且密码正确
					response = getMessageFactory().createResponse(Response.OK, request);
					// 添加date头
					SIPDateHeader dateHeader = new SIPDateHeader();
					// 使用自己修改的
					WvpSipDate wvpSipDate = new WvpSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
					dateHeader.setDate(wvpSipDate);
					response.addHeader(dateHeader);

					ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(Expires.NAME);
					if (expiresHeader == null) {
						response = getMessageFactory().createResponse(Response.BAD_REQUEST, request);
						ServerTransaction serverTransaction = getServerTransaction(evt);
						serverTransaction.sendResponse(response);
						if (serverTransaction.getDialog() != null) serverTransaction.getDialog().delete();
						return;
					}
					// 添加Contact头
					response.addHeader(request.getHeader(ContactHeader.NAME));
					// 添加Expires头
					response.addHeader(request.getExpires());

					// 获取到通信地址等信息
					ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
					String received = viaHeader.getReceived();
					int rPort = viaHeader.getRPort();
					// 解析本地地址替代
					if (StringUtils.isEmpty(received) || rPort == -1) {
						received = viaHeader.getHost();
						rPort = viaHeader.getPort();
					}
					//

					if (device == null) {
						device = new Device();
						device.setStreamMode("UDP");
						device.setCharset("gb2312");
						device.setDeviceId(deviceId);
						device.setFirsRegister(true);
					}else {
						if (device.getOnline() == 0) {
							device.setFirsRegister(true);
						}
					}
					device.setIp(received);
					device.setPort(rPort);
					device.setHostAddress(received.concat(":").concat(String.valueOf(rPort)));
					// 注销成功
					if (expiresHeader.getExpires() == 0) {
						registerFlag = 2;
					}
					// 注册成功
					else {
						device.setExpires(expiresHeader.getExpires());
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
			}

			ServerTransaction serverTransaction = getServerTransaction(evt);
			serverTransaction.sendResponse(response);
			if (serverTransaction.getDialog() != null) serverTransaction.getDialog().delete();
			// 注册成功
			// 保存到redis
			if (registerFlag == 1 ) {
				logger.info("[{}] 注册成功! deviceId:" + device.getDeviceId(), requestAddress);
				publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_REGISTER);
				// 重新注册更新设备和通道，以免设备替换或更新后信息无法更新
				handler.onRegister(device);
			} else if (registerFlag == 2) {
				logger.info("[{}] 注销成功! deviceId:" + device.getDeviceId(), requestAddress);
				publisher.outlineEventPublish(device.getDeviceId(), VideoManagerConstants.EVENT_OUTLINE_UNREGISTER);
			}
		} catch (SipException | InvalidArgumentException | NoSuchAlgorithmException | ParseException e) {
			e.printStackTrace();
		}
		
	}

}
