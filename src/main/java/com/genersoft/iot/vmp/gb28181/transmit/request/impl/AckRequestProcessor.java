package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;

import gov.nist.javax.sip.header.CSeq;

/**    
 * @Description:ACK请求处理器  
 * @author: songww
 * @date:   2020年5月3日 下午5:31:45     
 */
@Component
public class AckRequestProcessor implements ISIPRequestProcessor {
	
	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(RequestEvent evt, SipLayer layer) {
		Request request = evt.getRequest();
		Dialog dialog = evt.getDialog();
		try {
			Request ackRequest = null;
			CSeq csReq = (CSeq) request.getHeader(CSeq.NAME);
			ackRequest = dialog.createAck(csReq.getSeqNumber());
			dialog.sendAck(ackRequest);
			System.out.println("send ack to callee:" + ackRequest.toString());
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		
	}

}
