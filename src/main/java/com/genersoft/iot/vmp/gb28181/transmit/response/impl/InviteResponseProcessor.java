package com.genersoft.iot.vmp.gb28181.transmit.response.impl;

import java.text.ParseException;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.javax.sip.header.CSeq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorFactory;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;


/**
 * @Description:处理INVITE响应
 * @author: swwheihei
 * @date: 2020年5月3日 下午4:43:52
 */
@Component
public class InviteResponseProcessor implements ISIPResponseProcessor {

	private final static Logger logger = LoggerFactory.getLogger(SIPProcessorFactory.class);

	/**
	 * 处理invite响应
	 * 
	 * @param evt 响应消息
	 * @throws ParseException
	 */
	@Override
	public void process(ResponseEvent evt, SipLayer layer, SipConfig config) throws ParseException {
		try {
			Response response = evt.getResponse();
			int statusCode = response.getStatusCode();
			// trying不会回复
			if (statusCode == Response.TRYING) {
			}
			// 成功响应
			// 下发ack
			if (statusCode == Response.OK) {
				Dialog dialog = evt.getDialog();
				CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
				Request reqAck = dialog.createAck(cseq.getSeqNumber());

				SipURI requestURI = (SipURI) reqAck.getRequestURI();
				ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
				requestURI.setHost(viaHeader.getHost());
				requestURI.setPort(viaHeader.getPort());
				reqAck.setRequestURI(requestURI);

				dialog.sendAck(reqAck);
			}
		} catch (InvalidArgumentException | SipException e) {
			e.printStackTrace();
		}
	}

}
