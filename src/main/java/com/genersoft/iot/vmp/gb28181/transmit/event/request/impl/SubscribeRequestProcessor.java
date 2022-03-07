package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.CmdType;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.task.GPSSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIP命令类型： SUBSCRIBE请求
 */
@Component
public class SubscribeRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private Logger logger = LoggerFactory.getLogger(SubscribeRequestProcessor.class);
	private String method = "SUBSCRIBE";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private ISIPCommanderForPlatform sipCommanderForPlatform;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private UserSetup userSetup;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**   
	 * 处理SUBSCRIBE请求  
	 * 
	 * @param evt
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

				Response response = null;
				response = getMessageFactory().createResponse(200, request);
				if (response != null) {
					ExpiresHeader expireHeader = getHeaderFactory().createExpiresHeader(30);
					response.setExpires(expireHeader);
				}
				logger.info("response : " + response.toString());
				ServerTransaction transaction = getServerTransaction(evt);
				if (transaction != null) {
					transaction.sendResponse(response);
					transaction.getDialog().delete();
					transaction.terminate();
				} else {
					logger.info("processRequest serverTransactionId is null.");
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 处理移动位置订阅消息
	 */
	private void processNotifyMobilePosition(RequestEvent evt, Element rootElement) {
		String platformId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
		String deviceID = XmlUtil.getText(rootElement, "DeviceID");
		SubscribeInfo subscribeInfo = new SubscribeInfo(evt, platformId);
		String sn = XmlUtil.getText(rootElement, "SN");
		String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() +  "_MobilePosition_" + platformId;
		logger.info("接收到{}的MobilePosition订阅", platformId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>MobilePosition</CmdType>\r\n")
				.append("<SN>" + sn + "</SN>\r\n")
				.append("<DeviceID>" + deviceID + "</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");

		if (subscribeInfo.getExpires() > 0) {
			if (redisCatchStorage.getSubscribe(key) != null) {
				dynamicTask.stop(key);
			}
			String interval = XmlUtil.getText(rootElement, "Interval"); // GPS上报时间间隔
			dynamicTask.startCron(key, new GPSSubscribeTask(redisCatchStorage, sipCommanderForPlatform, storager,  platformId, sn, key), Integer.parseInt(interval));

			redisCatchStorage.updateSubscribe(key, subscribeInfo);
		}else if (subscribeInfo.getExpires() == 0) {
			dynamicTask.stop(key);
			redisCatchStorage.delSubscribe(key);
		}

		try {
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
			Response response = responseXmlAck(evt, resultXml.toString(), parentPlatform);
			ToHeader toHeader = (ToHeader)response.getHeader(ToHeader.NAME);
			subscribeInfo.setToTag(toHeader.getTag());
			redisCatchStorage.updateSubscribe(key, subscribeInfo);

		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void processNotifyAlarm(RequestEvent evt, Element rootElement) {

	}

	private void processNotifyCatalogList(RequestEvent evt, Element rootElement) {
		String platformId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
		String deviceID = XmlUtil.getText(rootElement, "DeviceID");
		SubscribeInfo subscribeInfo = new SubscribeInfo(evt, platformId);
		String sn = XmlUtil.getText(rootElement, "SN");
		String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() +  "_Catalog_" + platformId;
		logger.info("接收到{}的Catalog订阅", platformId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>Catalog</CmdType>\r\n")
				.append("<SN>" + sn + "</SN>\r\n")
				.append("<DeviceID>" + deviceID + "</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");

		if (subscribeInfo.getExpires() > 0) {
			redisCatchStorage.updateSubscribe(key, subscribeInfo);
		}else if (subscribeInfo.getExpires() == 0) {
			redisCatchStorage.delSubscribe(key);
		}

		try {
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
			Response response = responseXmlAck(evt, resultXml.toString(), parentPlatform);
			ToHeader toHeader = (ToHeader)response.getHeader(ToHeader.NAME);
			subscribeInfo.setToTag(toHeader.getTag());
			redisCatchStorage.updateSubscribe(key, subscribeInfo);

		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
