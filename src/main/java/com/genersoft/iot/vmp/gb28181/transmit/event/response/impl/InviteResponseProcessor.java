package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.UserAgentHeader;
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
	private VideoStreamSessionManager streamSession;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private SipFactory sipFactory;

	@Autowired
	private GitUtil gitUtil;

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
				SIPDialog dialog = new SIPDialog((SIPClientTransaction) event.getClientTransaction(), (SIPResponse) event.getResponse());
				CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
				Request reqAck = dialog.createAck(cseq.getSeqNumber());
				SipURI requestURI = (SipURI) reqAck.getRequestURI();
				String contentString = new String(response.getRawContent());
				// jainSip不支持y=字段， 移除以解析。
				int ssrcIndex = contentString.indexOf("y=");
				// 检查是否有y字段
				SessionDescription sdp;
				if (ssrcIndex >= 0) {
					//ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段
					String substring = contentString.substring(0, contentString.indexOf("y="));
					sdp = SdpFactory.getInstance().createSessionDescription(substring);
				} else {
					sdp = SdpFactory.getInstance().createSessionDescription(contentString);
				}
				requestURI.setUser(sdp.getOrigin().getUsername());
				try {
					requestURI.setHost(event.getRemoteIpAddress());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				requestURI.setPort(event.getRemotePort());
				reqAck.setRequestURI(requestURI);
				UserAgentHeader userAgentHeader = SipUtils.createUserAgentHeader(sipFactory, gitUtil);
				reqAck.addHeader(userAgentHeader);
				Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getIp()+":"+sipConfig.getPort()));
				reqAck.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
				logger.info("[回复ack] {}-> {}:{} ",requestURI, event.getRemoteIpAddress(), event.getRemotePort());

				dialog.sendAck(reqAck);

			}
		} catch (InvalidArgumentException | SipException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (SdpParseException e) {
			throw new RuntimeException(e);
		}
	}

}
