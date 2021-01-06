package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.validation.constraints.NotNull;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Host;
import org.springframework.util.DigestUtils;

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
	
	@Autowired
	@Qualifier(value="tcpSipProvider")
	private SipProvider tcpSipProvider;
	
	@Autowired
	@Qualifier(value="udpSipProvider")
	private SipProvider udpSipProvider;
	
	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag) throws ParseException, InvalidArgumentException, PeerUnavailableException {
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
		// callid
		CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
				: udpSipProvider.getNewCallId();
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
	
	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, String ssrc) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(channelId, device.getHostAddress());
		//via
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

		//callid
		CallIdHeader callIdHeader = null;
		if(device.getTransport().equals("TCP")) {
			callIdHeader = tcpSipProvider.getNewCallId();
		}
		if(device.getTransport().equals("UDP")) {
			callIdHeader = udpSipProvider.getNewCallId();
		}
		
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
	
	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag) throws ParseException, InvalidArgumentException, PeerUnavailableException {
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

		//callid
		CallIdHeader callIdHeader = null;
		if(device.getTransport().equals("TCP")) {
			callIdHeader = tcpSipProvider.getNewCallId();
		}
		if(device.getTransport().equals("UDP")) {
			callIdHeader = udpSipProvider.getNewCallId();
		}
		
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


	public Request createRegisterRequest(@NotNull ParentPlatform platform, long CSeq, String fromTag, String viaTag) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		String sipAddress = sipConfig.getSipIp() + ":" + sipConfig.getSipPort();
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(platform.getDeviceGBId(),
				platform.getServerIP() + ":" + platform.getServerPort());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(platform.getServerIP(), platform.getServerPort(), platform.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(platform.getDeviceGBId(),sipAddress);
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(platform.getDeviceGBId(),sipAddress);
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,null);

		//callid
		CallIdHeader callIdHeader = null;
		if(platform.getTransport().equals("TCP")) {
			callIdHeader = tcpSipProvider.getNewCallId();
		}
		if(platform.getTransport().equals("UDP")) {
			callIdHeader = udpSipProvider.getNewCallId();
		}

		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(CSeq, Request.REGISTER);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.REGISTER, callIdHeader,
				cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory()
				.createSipURI(platform.getDeviceGBId(), sipAddress));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		ExpiresHeader expires = sipFactory.createHeaderFactory().createExpiresHeader(Integer.parseInt(platform.getExpires()));
		request.addHeader(expires);

		return request;
	}

	public Request createRegisterRequest(@NotNull ParentPlatform parentPlatform, String fromTag, String viaTag,
										 String callId, String realm, String nonce, String scheme) throws ParseException, PeerUnavailableException, InvalidArgumentException {
		Request registerRequest = createRegisterRequest(parentPlatform, 2L, fromTag, viaTag);

		CallIdHeader callIdHeader = (CallIdHeader)registerRequest.getHeader(CallIdHeader.NAME);
		callIdHeader.setCallId(callId);

		String uri = "sip:" + parentPlatform.getServerGBId() +
				"@" + parentPlatform.getServerIP() +
				":" + parentPlatform.getServerPort();

		String HA1 = DigestUtils.md5DigestAsHex((parentPlatform.getDeviceGBId() + ":" + realm + ":" + parentPlatform.getPassword()).getBytes());
		String HA2=DigestUtils.md5DigestAsHex((Request.REGISTER + ":" + uri).getBytes());
		String RESPONSE = DigestUtils.md5DigestAsHex((HA1 + ":" + nonce + ":" +  HA2).getBytes());

		String authorizationHeaderContent = scheme + " username=\"" + parentPlatform.getDeviceGBId() + "\", " + "realm=\""
				+ realm + "\", nonce=\"" + nonce + "\", uri=\"" + uri  + "\", response=\"" + RESPONSE + "\"" + ", algorithm=MD5";
		AuthorizationHeader authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader(authorizationHeaderContent);
		registerRequest.addHeader(authorizationHeader);

		return registerRequest;
	}

//	public Request createKeetpaliveMessageRequest(ParentPlatform parentPlatform, String content, String fromTag, String toTag, Object o) throws PeerUnavailableException, ParseException, InvalidArgumentException {
//		Request request = null;
//		// sipuri
//		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP() + ":" + parentPlatform.getServerPort());
//		// via
//		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
//		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(),
//				parentPlatform.getTransport(), null);
//		viaHeader.setRPort();
//		viaHeaders.add(viaHeader);
//		// from
//		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(),
//				sipConfig.getSipIp() + ":" + sipConfig.getSipPort());
//		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
//		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
//		// to
//		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerGBDomain());
//		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
//		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);
//		// callid
//		CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
//				: udpSipProvider.getNewCallId();
//		// Forwards
//		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
//		// ceq
//		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.MESSAGE);
//
//		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
//				toHeader, viaHeaders, maxForwards);
//		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "MANSCDP+xml");
//		request.setContent(content, contentTypeHeader);
//		return request;
//	}
}
