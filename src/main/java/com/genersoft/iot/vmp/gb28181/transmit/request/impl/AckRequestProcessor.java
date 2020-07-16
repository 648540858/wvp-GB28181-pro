package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Request;

import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;

import gov.nist.javax.sip.header.CSeq;

/**    
 * @Description:ACK请求处理器  
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:31:45     
 */
public class AckRequestProcessor extends SIPRequestAbstractProcessor {
	
	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(RequestEvent evt) {
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
