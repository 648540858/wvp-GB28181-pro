package com.genersoft.iot.vmp.gb28181.transmit;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.AckRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.ByeRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.CancelRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.InviteRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.MessageRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.OtherRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.RegisterRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.SubscribeRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.ByeResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.CancelResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.InviteResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.OtherResponseProcessor;

/**    
 * @Description:TODO(这里用一句话描述这个类的作用)   
 * @author: swwheihei
 * @date:   2020年5月3日 下午4:24:37     
 */
@Component
public class SIPProcessorFactory {
	
	@Autowired
	private InviteRequestProcessor inviteRequestProcessor;
	
	@Autowired
	private RegisterRequestProcessor registerRequestProcessor;
	
	@Autowired
	private SubscribeRequestProcessor subscribeRequestProcessor;
	
	@Autowired
	private AckRequestProcessor ackRequestProcessor;
	
	@Autowired
	private ByeRequestProcessor byeRequestProcessor;
	
	@Autowired
	private CancelRequestProcessor cancelRequestProcessor;
	
	@Autowired
	private MessageRequestProcessor messageRequestProcessor;
	
	@Autowired
	private OtherRequestProcessor otherRequestProcessor;
	
	@Autowired
	private InviteResponseProcessor inviteResponseProcessor;
	
	@Autowired
	private ByeResponseProcessor byeResponseProcessor;
	
	@Autowired
	private CancelResponseProcessor cancelResponseProcessor;
	
	@Autowired
	private OtherResponseProcessor otherResponseProcessor;
	
	
	public ISIPRequestProcessor createRequestProcessor(RequestEvent evt) {
		Request request = evt.getRequest();
		String method = request.getMethod();
		
		if (Request.INVITE.equals(method)) {
			return inviteRequestProcessor;
		} else if (Request.REGISTER.equals(method)) {
			return registerRequestProcessor;
		} else if (Request.SUBSCRIBE.equals(method)) {
			return subscribeRequestProcessor;
		} else if (Request.ACK.equals(method)) {
			return ackRequestProcessor;
		} else if (Request.BYE.equals(method)) {
			return byeRequestProcessor;
		} else if (Request.CANCEL.equals(method)) {
			return cancelRequestProcessor;
		} else if (Request.MESSAGE.equals(method)) {
			return messageRequestProcessor;
		} else {
			return otherRequestProcessor;
		}
	}
	
	public ISIPResponseProcessor createResponseProcessor(ResponseEvent evt) {
		Response response = evt.getResponse();
		CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
		String method = cseqHeader.getMethod();
		if(Request.INVITE.equals(method)){
			return inviteResponseProcessor;
		} else if (Request.BYE.equals(method)) {
			return byeResponseProcessor;
		} else if (Request.CANCEL.equals(method)) {
			return cancelResponseProcessor;
		} else {
			return otherResponseProcessor;
		}
	}
	
}
