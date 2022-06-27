package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.stack.SIPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;


/**
 * @description: 处理INVITE响应
 * @author: panlinlin
 * @date: 2021年11月5日 16：40
 */
@Component
public class InviteResponseProcessor extends SIPResponseProcessorAbstract {

	private final static Logger logger = LoggerFactory.getLogger(InviteResponseProcessor.class);
	private final String method = "INVITE";

	@Autowired
	private SipLayer sipLayer;

	@Autowired
	private SipConfig config;


	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addResponseProcessor(method, this);
	}

	@Autowired
	private VideoStreamSessionManager streamSession;

	/**
	 * 处理invite响应
	 * 
	 * @param evt 响应消息
	 * @throws ParseException
	 */
	@Override
	public void process(ResponseEvent evt ){
		try {
			Response response = evt.getResponse();
			int statusCode = response.getStatusCode();
			// trying不会回复
			if (statusCode == Response.TRYING) {
			}
			// 成功响应
			// 下发ack
			if (statusCode == Response.OK) {
				ResponseEventExt event = (ResponseEventExt)evt;
				SIPDialog dialog = (SIPDialog)evt.getDialog();
				CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
				Request reqAck = dialog.createAck(cseq.getSeqNumber());
				SipURI requestURI = (SipURI) reqAck.getRequestURI();
				try {
					requestURI.setHost(event.getRemoteIpAddress());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				requestURI.setPort(event.getRemotePort());
				reqAck.setRequestURI(requestURI);
				logger.info("[回复ack] {}-> {}:{} ",requestURI, event.getRemoteIpAddress(), event.getRemotePort());

				dialog.sendAck(reqAck);

			}
		} catch (InvalidArgumentException | SipException e) {
			e.printStackTrace();
		}
	}

}
