package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIP命令类型： SUBSCRIBE请求
 * @author lin
 */
@Slf4j
@Component
public class SubscribeRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "SUBSCRIBE";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private SubscribeHolder subscribeHolder;

	@Autowired
	private SIPSender sipSender;


	@Autowired
	private IPlatformService platformService;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * 处理SUBSCRIBE请求
	 *
	 * @param evt 事件
	 */
	@Override
	public void process(RequestEvent evt) {
		SIPRequest request = (SIPRequest) evt.getRequest();
		try {
			Element rootElement = getRootElement(evt);
			if (rootElement == null) {
				log.error("处理SUBSCRIBE请求  未获取到消息体{}", evt.getRequest());
				responseAck(request, Response.BAD_REQUEST);
				return;
			}
			String platformId = SipUtils.getUserIdFromFromHeader(request);
			String cmd = XmlUtil.getText(rootElement, "CmdType");
			log.info("[收到订阅请求] 类型： {}, 来自： {}", cmd, platformId);
			if (CmdType.MOBILE_POSITION.equals(cmd)) {
				processNotifyMobilePosition(request, rootElement);
//			} else if (CmdType.ALARM.equals(cmd)) {
//				logger.info("接收到Alarm订阅");
//				processNotifyAlarm(serverTransaction, rootElement);
			} else if (CmdType.CATALOG.equals(cmd)) {
				processNotifyCatalogList(request, rootElement);
			} else {
				log.info("接收到消息：" + cmd);

				Response response = getMessageFactory().createResponse(200, request);
				if (response != null) {
					ExpiresHeader expireHeader = getHeaderFactory().createExpiresHeader(30);
					response.setExpires(expireHeader);
				}
				log.info("response : " + response);
				sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			log.error("未处理的异常 ", e);
		}

	}

	/**
	 * 处理移动位置订阅消息
	 */
	private void processNotifyMobilePosition(SIPRequest request, Element rootElement) throws SipException {
		if (request == null) {
			return;
		}
		String platformId = SipUtils.getUserIdFromFromHeader(request);
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		Platform platform = platformService.queryPlatformByServerGBId(platformId);
		if (platform == null) {
			return;
		}

		String sn = XmlUtil.getText(rootElement, "SN");
		log.info("[回复上级的移动位置订阅请求]: {}", platformId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>MobilePosition</CmdType>\r\n")
				.append("<SN>").append(sn).append("</SN>\r\n")
				.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");



		try {
			int expires = request.getExpires().getExpires();
			SIPResponse response = responseXmlAck(request, resultXml.toString(), platform, expires);

			SubscribeInfo subscribeInfo = SubscribeInfo.getInstance(response, platformId, expires,
					(EventHeader)request.getHeader(EventHeader.NAME));
			if (subscribeInfo.getExpires() > 0) {
				// GPS上报时间间隔
				String interval = XmlUtil.getText(rootElement, "Interval");
				if (interval == null) {
					subscribeInfo.setGpsInterval(5);
				}else {
					subscribeInfo.setGpsInterval(Integer.parseInt(interval));
				}
				subscribeInfo.setSn(sn);
			}
			if (subscribeInfo.getExpires() == 0) {
				subscribeHolder.removeMobilePositionSubscribe(platformId);
			}else {
				subscribeInfo.setTransactionInfo(new SipTransactionInfo(response));
				subscribeHolder.putMobilePositionSubscribe(platformId, subscribeInfo, ()->{
					platformService.sendNotifyMobilePosition(platformId);
				});
			}

		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("未处理的异常 ", e);
		}
	}

	private void processNotifyAlarm(RequestEvent evt, Element rootElement) {

	}

	private void processNotifyCatalogList(SIPRequest request, Element rootElement) throws SipException {
		if (request == null) {
			log.info("[处理目录订阅] 发现request为NUll。已忽略");
			return;
		}
		String platformId = SipUtils.getUserIdFromFromHeader(request);
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		Platform platform = platformService.queryPlatformByServerGBId(platformId);
		if (platform == null){
			log.info("[处理目录订阅] 未找到平台 {}。已忽略", platformId);
			return;
		}

		String sn = XmlUtil.getText(rootElement, "SN");
		log.info("[回复上级的目录订阅请求]: {}/{}", platformId, deviceId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>Catalog</CmdType>\r\n")
				.append("<SN>").append(sn).append("</SN>\r\n")
				.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");


		try {
			int expires = request.getExpires().getExpires();
			Platform parentPlatform = platformService.queryPlatformByServerGBId(platformId);
			SIPResponse response = responseXmlAck(request, resultXml.toString(), parentPlatform, expires);

			SubscribeInfo subscribeInfo = SubscribeInfo.getInstance(response, platformId, expires,
					(EventHeader)request.getHeader(EventHeader.NAME));

			if (subscribeInfo.getExpires() == 0) {
				subscribeHolder.removeCatalogSubscribe(platformId);
			}else {
				subscribeInfo.setTransactionInfo(new SipTransactionInfo(response));
				subscribeHolder.putCatalogSubscribe(platformId, subscribeInfo);
			}
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("未处理的异常 ", e);
		}
		if (subscribeHolder.getCatalogSubscribe(platformId) == null
				&& platform.getAutoPushChannel() != null && platform.getAutoPushChannel()) {
			platformService.addSimulatedSubscribeInfo(platform);
		}
	}
}
