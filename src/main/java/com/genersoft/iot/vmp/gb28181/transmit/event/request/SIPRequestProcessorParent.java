package com.genersoft.iot.vmp.gb28181.transmit.event.request;

import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.ByteArrayInputStream;
import java.text.ParseException;

/**    
 * @description:处理接收IPCamera发来的SIP协议请求消息
 * @author: songww
 * @date:   2020年5月3日 下午4:42:22     
 */
public abstract class SIPRequestProcessorParent {

	private final static Logger logger = LoggerFactory.getLogger(SIPRequestProcessorParent.class);

	@Autowired
	@Qualifier(value="tcpSipProvider")
	private SipProviderImpl tcpSipProvider;

	@Autowired
	@Qualifier(value="udpSipProvider")
	private SipProviderImpl udpSipProvider;

	/**
	 * 根据 RequestEvent 获取 ServerTransaction
	 * @param evt
	 * @return
	 */
	public ServerTransaction getServerTransaction(RequestEvent evt) {
		Request request = evt.getRequest();
		ServerTransaction serverTransaction = evt.getServerTransaction();
		// 判断TCP还是UDP
		boolean isTcp = false;
		ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
		String transport = reqViaHeader.getTransport();
		if (transport.equals("TCP")) {
			isTcp = true;
		}

		if (serverTransaction == null) {
			try {
				if (isTcp) {
					SipStackImpl stack = (SipStackImpl)tcpSipProvider.getSipStack();
					serverTransaction = (SIPServerTransaction) stack.findTransaction((SIPRequest)request, true);
					if (serverTransaction == null) {
						serverTransaction = tcpSipProvider.getNewServerTransaction(request);
					}
				} else {
					SipStackImpl stack = (SipStackImpl)udpSipProvider.getSipStack();
					serverTransaction = (SIPServerTransaction) stack.findTransaction((SIPRequest)request, true);
					if (serverTransaction == null) {
						serverTransaction = udpSipProvider.getNewServerTransaction(request);
					}
				}
			} catch (TransactionAlreadyExistsException e) {
				logger.error(e.getMessage());
			} catch (TransactionUnavailableException e) {
				logger.error(e.getMessage());
			}
		}
		return serverTransaction;
	}
	
	public AddressFactory getAddressFactory() {
		try {
			return SipFactory.getInstance().createAddressFactory();
		} catch (PeerUnavailableException e) {
			e.printStackTrace();
		}
		return null;
	}

	public HeaderFactory getHeaderFactory() {
		try {
			return SipFactory.getInstance().createHeaderFactory();
		} catch (PeerUnavailableException e) {
			e.printStackTrace();
		}
		return null;
	}

	public MessageFactory getMessageFactory() {
		try {
			return SipFactory.getInstance().createMessageFactory();
		} catch (PeerUnavailableException e) {
			e.printStackTrace();
		}
		return null;
	}

	/***
	 * 回复状态码
	 * 100 trying
	 * 200 OK
	 * 400
	 * 404
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	public void responseAck(RequestEvent evt, int statusCode) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(statusCode, evt.getRequest());
		ServerTransaction serverTransaction = getServerTransaction(evt);
		serverTransaction.sendResponse(response);
		if (statusCode >= 200 && !"NOTIFY".equals(evt.getRequest().getMethod())) {

			if (serverTransaction.getDialog() != null) serverTransaction.getDialog().delete();
		}
	}

	public void responseAck(RequestEvent evt, int statusCode, String msg) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(statusCode, evt.getRequest());
		response.setReasonPhrase(msg);
		ServerTransaction serverTransaction = getServerTransaction(evt);
		serverTransaction.sendResponse(response);
		if (statusCode >= 200 && !"NOTIFY".equals(evt.getRequest().getMethod())) {
			if (serverTransaction.getDialog() != null) serverTransaction.getDialog().delete();
		}
	}

	/**
	 * 回复带sdp的200
	 * @param evt
	 * @param sdp
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	public void responseAck(RequestEvent evt, String sdp) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		SipFactory sipFactory = SipFactory.getInstance();
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		response.setContent(sdp, contentTypeHeader);

		SipURI sipURI = (SipURI)evt.getRequest().getRequestURI();

		Address concatAddress = sipFactory.createAddressFactory().createAddress(
				sipFactory.createAddressFactory().createSipURI(sipURI.getUser(),  sipURI.getHost()+":"+sipURI.getPort()
				));
		response.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
		getServerTransaction(evt).sendResponse(response);
	}

	public Element getRootElement(RequestEvent evt) throws DocumentException {
		return getRootElement(evt, "gb2312");
	}
	public Element getRootElement(RequestEvent evt, String charset) throws DocumentException {
		if (charset == null) charset = "gb2312";
		Request request = evt.getRequest();
		SAXReader reader = new SAXReader();
		reader.setEncoding(charset);
		Document xml = reader.read(new ByteArrayInputStream(request.getRawContent()));
		return xml.getRootElement();
	}

}
