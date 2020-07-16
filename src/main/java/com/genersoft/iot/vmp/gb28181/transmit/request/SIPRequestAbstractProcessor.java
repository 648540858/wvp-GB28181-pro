package com.genersoft.iot.vmp.gb28181.transmit.request;

import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPServerTransaction;

/**    
 * @Description:处理接收IPCamera发来的SIP协议请求消息
 * @author: songww
 * @date:   2020年5月3日 下午4:42:22     
 */
public abstract class SIPRequestAbstractProcessor implements ISIPRequestProcessor {

	protected RequestEvent evt;
	
	private SipProvider tcpSipProvider;
	
	private SipProvider udpSipProvider;
	
	@Override
	public void process() {
		this.process(evt);
	}
	
	public abstract void process(RequestEvent evt);
	
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
				e.printStackTrace();
			} catch (TransactionUnavailableException e) {
				e.printStackTrace();
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

	public RequestEvent getRequestEvent() {
		return evt;
	}

	public void setRequestEvent(RequestEvent evt) {
		this.evt = evt;
	}

	public SipProvider getTcpSipProvider() {
		return tcpSipProvider;
	}

	public void setTcpSipProvider(SipProvider tcpSipProvider) {
		this.tcpSipProvider = tcpSipProvider;
	}

	public SipProvider getUdpSipProvider() {
		return udpSipProvider;
	}

	public void setUdpSipProvider(SipProvider udpSipProvider) {
		this.udpSipProvider = udpSipProvider;
	}
	
	
}
