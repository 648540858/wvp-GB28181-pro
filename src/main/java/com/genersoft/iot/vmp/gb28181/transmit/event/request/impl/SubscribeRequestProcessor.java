package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CmdType;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeHandlerTask;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPServerTransactionImpl;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIP命令类型： SUBSCRIBE请求
 * @author lin
 */
@Component
public class SubscribeRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final Logger logger = LoggerFactory.getLogger(SubscribeRequestProcessor.class);
	private final String method = "SUBSCRIBE";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private SubscribeHolder subscribeHolder;

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
		ServerTransaction serverTransaction = getServerTransaction(evt);
		Request request = evt.getRequest();
		try {
			Element rootElement = getRootElement(evt);
			if (rootElement == null) {
				logger.error("处理SUBSCRIBE请求  未获取到消息体{}", evt.getRequest());
				return;
			}
			String cmd = XmlUtil.getText(rootElement, "CmdType");
			if (CmdType.MOBILE_POSITION.equals(cmd)) {
				processNotifyMobilePosition(serverTransaction, rootElement);
//			} else if (CmdType.ALARM.equals(cmd)) {
//				logger.info("接收到Alarm订阅");
//				processNotifyAlarm(serverTransaction, rootElement);
			} else if (CmdType.CATALOG.equals(cmd)) {
				processNotifyCatalogList(serverTransaction, rootElement);
			} else {
				logger.info("接收到消息：" + cmd);

				Response response = getMessageFactory().createResponse(200, request);
				if (response != null) {
					ExpiresHeader expireHeader = getHeaderFactory().createExpiresHeader(30);
					response.setExpires(expireHeader);
				}
				logger.info("response : " + response);
				ServerTransaction transaction = getServerTransaction(evt);
				if (transaction != null) {
					transaction.sendResponse(response);
					transaction.terminate();
				} else {
					logger.info("processRequest serverTransactionId is null.");
				}
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 处理移动位置订阅消息
	 */
	private void processNotifyMobilePosition(ServerTransaction serverTransaction, Element rootElement) throws SipException {
		if (serverTransaction == null) {
			return;
		}
		String platformId = SipUtils.getUserIdFromFromHeader(serverTransaction.getRequest());
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
		SubscribeInfo subscribeInfo = new SubscribeInfo(serverTransaction, platformId);
		if (platform == null) {
			return;
		}

		String sn = XmlUtil.getText(rootElement, "SN");
		logger.info("[回复上级的移动位置订阅请求]: {}", platformId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>MobilePosition</CmdType>\r\n")
				.append("<SN>").append(sn).append("</SN>\r\n")
				.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");

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

		try {
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
			SIPResponse response = responseXmlAck(serverTransaction, resultXml.toString(), parentPlatform, subscribeInfo.getExpires());
			if (subscribeInfo.getExpires() == 0) {
				subscribeHolder.removeMobilePositionSubscribe(platformId);
			}else {
				subscribeInfo.setResponse(response);
				subscribeHolder.putMobilePositionSubscribe(platformId, subscribeInfo);
			}

		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	private void processNotifyAlarm(RequestEvent evt, Element rootElement) {

	}

	private void processNotifyCatalogList(ServerTransaction serverTransaction, Element rootElement) throws SipException {
		if (serverTransaction == null) {
			return;
		}
		String platformId = SipUtils.getUserIdFromFromHeader(serverTransaction.getRequest());
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
		if (platform == null){
			return;
		}
		SubscribeInfo subscribeInfo = new SubscribeInfo(serverTransaction, platformId);

		String sn = XmlUtil.getText(rootElement, "SN");
		logger.info("[回复上级的目录订阅请求]: {}/{}", platformId, deviceId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>Catalog</CmdType>\r\n")
				.append("<SN>").append(sn).append("</SN>\r\n")
				.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");

		if (subscribeInfo.getExpires() > 0) {
			subscribeHolder.putCatalogSubscribe(platformId, subscribeInfo);
		}else if (subscribeInfo.getExpires() == 0) {
			subscribeHolder.removeCatalogSubscribe(platformId);
		}
		try {
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
			SIPResponse response = responseXmlAck(serverTransaction, resultXml.toString(), parentPlatform, subscribeInfo.getExpires());
			if (subscribeInfo.getExpires() == 0) {
				subscribeHolder.removeCatalogSubscribe(platformId);
			}else {
				subscribeInfo.setResponse(response);
				subscribeHolder.putCatalogSubscribe(platformId, subscribeInfo);
			}
		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}
}
