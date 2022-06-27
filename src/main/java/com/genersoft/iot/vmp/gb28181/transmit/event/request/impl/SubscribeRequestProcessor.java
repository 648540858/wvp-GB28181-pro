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

	@Lazy
	@Autowired
	@Qualifier(value="tcpSipProvider")
	private SipProviderImpl tcpSipProvider;

	@Lazy
	@Autowired
	@Qualifier(value="udpSipProvider")
	private SipProviderImpl udpSipProvider;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private UserSetting userSetting;

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
		Request request = evt.getRequest();
		try {
			Element rootElement = getRootElement(evt);
			String cmd = XmlUtil.getText(rootElement, "CmdType");
			if (CmdType.MOBILE_POSITION.equals(cmd)) {
				processNotifyMobilePosition(evt, rootElement);
//			} else if (CmdType.ALARM.equals(cmd)) {
//				logger.info("接收到Alarm订阅");
//				processNotifyAlarm(evt, rootElement);
			} else if (CmdType.CATALOG.equals(cmd)) {
				processNotifyCatalogList(evt, rootElement);
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
					transaction.getDialog().delete();
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
	private void processNotifyMobilePosition(RequestEvent evt, Element rootElement) throws SipException {
		String platformId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
		SubscribeInfo subscribeInfo = new SubscribeInfo(evt, platformId);
		if (platform == null) {
			return;
		}
		if (evt.getServerTransaction() == null) {
			ServerTransaction serverTransaction = "TCP".equals(platform.getTransport()) ? tcpSipProvider.getNewServerTransaction(evt.getRequest())
					: udpSipProvider.getNewServerTransaction(evt.getRequest());
			subscribeInfo.setTransaction(serverTransaction);
			Dialog dialog = serverTransaction.getDialog();
			dialog.terminateOnBye(false);
			subscribeInfo.setDialog(dialog);
		}
		String sn = XmlUtil.getText(rootElement, "SN");
		logger.info("[回复 移动位置订阅]: {}", platformId);
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
			subscribeHolder.putMobilePositionSubscribe(platformId, subscribeInfo);

		}else if (subscribeInfo.getExpires() == 0) {
			subscribeHolder.removeMobilePositionSubscribe(platformId);
		}

		try {
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
			responseXmlAck(evt, resultXml.toString(), parentPlatform);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	private void processNotifyAlarm(RequestEvent evt, Element rootElement) {

	}

	private void processNotifyCatalogList(RequestEvent evt, Element rootElement) throws SipException {

		System.out.println(evt.getRequest().toString());
		String platformId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
		if (platform == null){
			return;
		}
		SubscribeInfo subscribeInfo = new SubscribeInfo(evt, platformId);
		if (evt.getServerTransaction() == null) {
			ServerTransaction serverTransaction = "TCP".equals(platform.getTransport()) ? tcpSipProvider.getNewServerTransaction(evt.getRequest())
					: udpSipProvider.getNewServerTransaction(evt.getRequest());
			subscribeInfo.setTransaction(serverTransaction);
			Dialog dialog = serverTransaction.getDialog();
			dialog.terminateOnBye(false);
			subscribeInfo.setDialog(dialog);
		}
		String sn = XmlUtil.getText(rootElement, "SN");
		logger.info("[回复 目录订阅]: {}/{}", platformId, deviceId);
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
			responseXmlAck(evt, resultXml.toString(), parentPlatform);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

}
