package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;

import java.text.ParseException;

/**    
 * @Description: BYE请求处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:05     
 */
public class ByeRequestProcessor extends SIPRequestAbstractProcessor {

	/**
	 * 处理BYE请求
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		try {
			responseAck(evt);
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// TODO 优先级99 Bye Request消息实现，此消息一般为级联消息，上级给下级发送视频停止指令
		
	}

	/***
	 * 回复200 OK
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void responseAck(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

}
