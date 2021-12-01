package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
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

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		
	}

}
