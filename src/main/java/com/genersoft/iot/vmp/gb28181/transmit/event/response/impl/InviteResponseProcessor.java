package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181Sdp;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.SipURI;
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
	private SIPProcessorObserver sipProcessorObserver;


	@Autowired
	private SipLayer sipLayer;

	@Autowired
	private SIPSender sipSender;

	@Autowired
	private SIPRequestHeaderProvider headerProvider;


	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addResponseProcessor(method, this);
	}



	/**
	 * 处理invite响应
	 * 
	 * @param evt 响应消息
	 * @throws ParseException
	 */
	@Override
	public void process(ResponseEvent evt ){
		logger.debug("接收到消息：" + evt.getResponse());
		try {
			SIPResponse response = (SIPResponse)evt.getResponse();
			int statusCode = response.getStatusCode();
			// trying不会回复
			if (statusCode == Response.TRYING) {
			}
			// 成功响应
			// 下发ack
			if (statusCode == Response.OK) {
				ResponseEventExt event = (ResponseEventExt)evt;

				String contentString = new String(response.getRawContent());
				Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
				SessionDescription sdp = gb28181Sdp.getBaseSdb();
				SipURI requestUri = SipFactory.getInstance().createAddressFactory().createSipURI(sdp.getOrigin().getUsername(), event.getRemoteIpAddress() + ":" + event.getRemotePort());
				Request reqAck = headerProvider.createAckRequest(response.getLocalAddress().getHostAddress(), requestUri, response);

				logger.info("[回复ack] {}-> {}:{} ", sdp.getOrigin().getUsername(), event.getRemoteIpAddress(), event.getRemotePort());
				sipSender.transmitRequest( response.getLocalAddress().getHostAddress(), reqAck);
			}
		} catch (InvalidArgumentException | ParseException | SipException | SdpParseException e) {
			logger.info("[点播回复ACK]，异常：", e );
		}
	}

}
