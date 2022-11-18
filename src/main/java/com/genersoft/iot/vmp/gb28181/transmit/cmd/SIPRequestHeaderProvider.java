package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;

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
	private SipLayer sipLayer;

	@Autowired
	private GitUtil gitUtil;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private VideoStreamSessionManager streamSession;
	
	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.MESSAGE);

		request = sipLayer.getSipFactory().createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		ContentTypeHeader contentTypeHeader = sipLayer.getSipFactory().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, String ssrc, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		HeaderFactory headerFactory = sipLayer.getSipFactory().createHeaderFactory();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);

		//from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress,null);
		
		//Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.INVITE);
		request = sipLayer.getSipFactory().createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		// Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipLayer.getSipFactory().createHeaderFactory().createContactHeader(concatAddress));
		// Subject
		SubjectHeader subjectHeader = sipLayer.getSipFactory().createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);
		ContentTypeHeader contentTypeHeader = sipLayer.getSipFactory().createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader, String ssrc) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress,null);
		
		//Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.INVITE);
		request = sipLayer.getSipFactory().createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		// Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipLayer.getSipFactory().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		// Subject
		SubjectHeader subjectHeader = sipLayer.getSipFactory().createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);

		ContentTypeHeader contentTypeHeader = sipLayer.getSipFactory().createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createByteRequest(Device device, String channelId, SipTransactionInfo transactionInfo) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), SipUtils.getNewViaTag());
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(),sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, transactionInfo.getFromTag());
		//to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId,device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress,	transactionInfo.getToTag());

		//Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.BYE);
		CallIdHeader callIdHeader = sipLayer.getSipFactory().createHeaderFactory().createCallIdHeader(transactionInfo.getCallId());
		request = sipLayer.getSipFactory().createMessageFactory().createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(sipLayer.getSipFactory().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		return request;
	}

	public Request createSubscribeRequest(Device device, String content, SIPRequest requestOld, Integer expires, String event, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(),
				device.getTransport(), SipUtils.getNewViaTag());
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, requestOld == null ? SipUtils.getNewFromTag() :requestOld.getFromTag());
		// to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress, requestOld == null ? null :requestOld.getToTag());

		// Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);

		// ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.SUBSCRIBE);

		request = sipLayer.getSipFactory().createMessageFactory().createRequest(requestURI, Request.SUBSCRIBE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);


		Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(sipLayer.getSipFactory().createHeaderFactory().createContactHeader(concatAddress));

		// Expires
		ExpiresHeader expireHeader = sipLayer.getSipFactory().createHeaderFactory().createExpiresHeader(expires);
		request.addHeader(expireHeader);

		// Event
		EventHeader eventHeader = sipLayer.getSipFactory().createHeaderFactory().createEventHeader(event);

		int random = (int) Math.floor(Math.random() * 10000);
		eventHeader.setEventId(random + "");
		request.addHeader(eventHeader);

		ContentTypeHeader contentTypeHeader = sipLayer.getSipFactory().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		return request;
	}

	public SIPRequest createInfoRequest(Device device, String channelId, String content, SipTransactionInfo transactionInfo)
			throws SipException, ParseException, InvalidArgumentException {
		if (device == null || transactionInfo == null) {
			return null;
		}
		SIPRequest request = null;
		//请求行
		SipURI requestLine = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), SipUtils.getNewViaTag());
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(),sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, transactionInfo.getFromTag());
		//to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId,device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress,	transactionInfo.getToTag());

		//Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.INFO);
		CallIdHeader callIdHeader = sipLayer.getSipFactory().createHeaderFactory().createCallIdHeader(transactionInfo.getCallId());
		request = (SIPRequest)sipLayer.getSipFactory().createMessageFactory().createRequest(requestLine, Request.INFO, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(sipLayer.getSipFactory().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		if (content != null) {
			ContentTypeHeader contentTypeHeader = sipLayer.getSipFactory().createHeaderFactory().createContentTypeHeader("Application",
					"MANSRTSP");
			request.setContent(content, contentTypeHeader);
		}
		return request;
	}

	public Request createAckRequest(String localIp, SipURI sipURI, SIPResponse sipResponse) throws ParseException, InvalidArgumentException, PeerUnavailableException {


		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(localIp, sipConfig.getPort(), sipResponse.getTopmostViaHeader().getTransport(), SipUtils.getNewViaTag());
		viaHeaders.add(viaHeader);

		//Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(sipResponse.getCSeqHeader().getSeqNumber(), Request.ACK);

		Request request = sipLayer.getSipFactory().createMessageFactory().createRequest(sipURI, Request.ACK, sipResponse.getCallIdHeader(), cSeqHeader, sipResponse.getFromHeader(), sipResponse.getToHeader(), viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		Address concatAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), localIp + ":"+sipConfig.getPort()));
		request.addHeader(sipLayer.getSipFactory().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		return request;
	}

	public Request createBroadcastMessageRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipLayer.getSipFactory().createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(sipConfig.getId(), sipConfig.getDomain());
		Address fromAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipLayer.getSipFactory().createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipLayer.getSipFactory().createAddressFactory().createSipURI(channelId, device.getHostAddress());
		Address toAddress = sipLayer.getSipFactory().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipLayer.getSipFactory().createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipLayer.getSipFactory().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.MESSAGE);

		ContentTypeHeader contentTypeHeader = sipLayer.getSipFactory().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");

		request = sipLayer.getSipFactory().createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards, contentTypeHeader, content);

		request.addHeader(SipUtils.createUserAgentHeader(sipLayer.getSipFactory(), gitUtil));

		return request;
	}
}
