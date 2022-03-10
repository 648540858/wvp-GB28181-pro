package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;

/**
 * @description:摄像头命令request创造器 TODO 冗余代码太多待优化
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
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private VideoStreamSessionManager streamSession;
	
	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getId(),
				sipConfig.getIp() + ":" + sipConfig.getPort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), sipConfig.getDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.MESSAGE);

		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, String ssrc, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(channelId, device.getHostAddress());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);

		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getId(),sipConfig.getDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId,sipConfig.getDomain()); 
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,null);
		
		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(Request.INVITE), Request.INVITE);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getIp()+":"+sipConfig.getPort()));
		// Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
		// Subject
		SubjectHeader subjectHeader = sipFactory.createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader, String ssrc) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getId(),sipConfig.getDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId, sipConfig.getDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,null);
		
		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(Request.INVITE), Request.INVITE);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getIp()+":"+sipConfig.getPort()));
		// Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
		// Subject
		SubjectHeader subjectHeader = sipFactory.createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);

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
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getId(),sipConfig.getDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId,sipConfig.getDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,toTag);

		//Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.BYE);
		CallIdHeader callIdHeader = sipFactory.createHeaderFactory().createCallIdHeader(callId);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getIp()+":"+sipConfig.getPort()));

		return request;
	}

	public Request createSubscribeRequest(Device device, String content, String viaTag, String fromTag, String toTag, Integer expires, String event, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(),
				device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getId(),
				sipConfig.getIp() + ":" + sipConfig.getPort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(), sipConfig.getDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(Request.SUBSCRIBE), Request.SUBSCRIBE);

		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.SUBSCRIBE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);


		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getIp()+":"+sipConfig.getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		// Expires
		ExpiresHeader expireHeader = sipFactory.createHeaderFactory().createExpiresHeader(expires);
		request.addHeader(expireHeader);

		// Event
		EventHeader eventHeader = sipFactory.createHeaderFactory().createEventHeader(event);

		int random = (int)Math.random() * 1000000000;
		eventHeader.setEventId(random + "");
		request.addHeader(eventHeader);

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createInfoRequest(Device device, StreamInfo streamInfo, String content, Long cseq)
			throws PeerUnavailableException, ParseException, InvalidArgumentException {
		Request request = null;
		if (streamInfo == null) return null;
		Dialog dialog = streamSession.getDialogByStream(streamInfo.getDeviceID(), streamInfo.getChannelId(), streamInfo.getStream());

		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(),
				device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(device.getIp(), device.getPort(),
				device.getTransport(), null);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getId(),
				sipConfig.getDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, dialog.getLocalTag());
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(streamInfo.getChannelId(),
				sipConfig.getDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, dialog.getRemoteTag());

		// callid
		CallIdHeader callIdHeader = dialog.getCallId();

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		if (cseq == null) {
			cseq = redisCatchStorage.getCSEQ(Request.INFO);
		}
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory()
				.createCSeqHeader(cseq, Request.INFO);

		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INFO, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory()
				.createSipURI(sipConfig.getId(), sipConfig.getIp() + ":" + sipConfig.getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application",
				"MANSRTSP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
}
