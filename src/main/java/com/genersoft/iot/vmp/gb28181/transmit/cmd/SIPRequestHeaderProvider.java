package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;

/**
 * @Description:摄像头命令request创造器 TODO 冗余代码太多待优化
 * @author: swwheihei
 * @date: 2020年5月6日 上午9:29:02
 */
@Component
public class SIPRequestHeaderProvider {

	@Autowired
	private SipConfig sipConfig;
	
	@Autowired
	private SipFactory sipFactory;
	
	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(),
				device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipIp() + ":" + sipConfig.getSipPort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.MESSAGE);

		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, String ssrc, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(channelId, device.getHostAddress());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(device.getIp(), device.getPort(), device.getTransport(), viaTag);
//		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);

		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),sipConfig.getSipDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId,sipConfig.getSipDomain()); 
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,null);
		
		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.INVITE);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(), sipConfig.getSipIp()+":"+sipConfig.getSipPort()));
		// Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(), device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
		// Subject
		SubjectHeader subjectHeader = sipFactory.createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getSipId(), 0));
		request.addHeader(subjectHeader);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(device.getIp(), device.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),sipConfig.getSipDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId,sipConfig.getSipDomain()); 
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,null);
		
		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.INVITE);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(), sipConfig.getSipIp()+":"+sipConfig.getSipPort()));
		// Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(), device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
		
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createByteRequest(Device device, String channelId, String viaTag, String fromTag, String toTag, String callId) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(), device.getTransport(), viaTag);
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),sipConfig.getSipDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId,sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,toTag);

		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.BYE);
		CallIdHeader callIdHeader = sipFactory.createHeaderFactory().createCallIdHeader(callId);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(), sipConfig.getSipIp()+":"+sipConfig.getSipPort()));

		return request;
	}

	public Request createSubscribeRequest(Device device, String content, String viaTag, String fromTag, String toTag, Integer expires, String event, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(),
				device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipIp() + ":" + sipConfig.getSipPort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.SUBSCRIBE);

		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.SUBSCRIBE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);


		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(), sipConfig.getSipIp()+":"+sipConfig.getSipPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		// Expires
		ExpiresHeader expireHeader = sipFactory.createHeaderFactory().createExpiresHeader(expires);
		request.addHeader(expireHeader);

		// Event
		EventHeader eventHeader = sipFactory.createHeaderFactory().createEventHeader(event);
		request.addHeader(eventHeader);

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}
}
