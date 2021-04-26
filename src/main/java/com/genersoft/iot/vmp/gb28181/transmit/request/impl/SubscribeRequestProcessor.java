package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**    
 * @Description:SUBSCRIBE请求处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:31:20     
 */
public class SubscribeRequestProcessor extends SIPRequestAbstractProcessor {

	private Logger logger = LoggerFactory.getLogger(SubscribeRequestProcessor.class);

	/**   
	 * 处理SUBSCRIBE请求  
	 * 
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
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
