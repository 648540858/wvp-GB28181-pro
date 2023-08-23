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
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:摄像头命令request创造器
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

	private SipURI getSipURI(Device device, String channelId) throws ParseException, PeerUnavailableException {
		return SipFactory.getInstance().createAddressFactory().createSipURI(channelId, device.getHostAddress());
	}

	public SipURI createSipURI(String user, String host) throws PeerUnavailableException, ParseException {
		AddressFactory addressFactory = SipFactory.getInstance().createAddressFactory();
		return addressFactory.createSipURI(user, host);
	}

	public FromHeader createFromHeader(String user, String host, String fromTag) throws PeerUnavailableException, ParseException {
		AddressFactory addressFactory = SipFactory.getInstance().createAddressFactory();
		SipURI fromSipURI = addressFactory.createSipURI(user, host);
		Address fromAddress = addressFactory.createAddress(fromSipURI);
		FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, fromTag);
		return fromHeader;
	}

	public List<ViaHeader> createVia(String ip, int port, String transport, String viaTag) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(ip, port, transport, viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		return viaHeaders;
	}

	public ToHeader createToHeader(String deviceId, String hostAddress, String toTag) throws PeerUnavailableException, ParseException {
		AddressFactory addressFactory = SipFactory.getInstance().createAddressFactory();
		SipURI toSipURI = addressFactory.createSipURI(deviceId, hostAddress);
		Address toAddress = addressFactory.createAddress(toSipURI);
		return SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress, toTag);
	}

	public CSeqHeader createCSeqHeader(long sequenceNumber, String method) throws PeerUnavailableException, InvalidArgumentException, ParseException {
		return SipFactory.getInstance().createHeaderFactory().createCSeqHeader(sequenceNumber, method);
	}

	private Address createAddress(String user, String host) throws PeerUnavailableException, ParseException {
		return SipFactory.getInstance().createAddressFactory().createAddress(SipFactory.getInstance().createAddressFactory().createSipURI(user, host));
	}

	private static MaxForwardsHeader getForwardsHeader() throws InvalidArgumentException, PeerUnavailableException {
		return SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);
	}

	private ContentTypeHeader createContentTypeHeader(String contentType, String contentSubType) throws ParseException, PeerUnavailableException {
		return SipFactory.getInstance().createHeaderFactory().createContentTypeHeader(contentType, contentSubType);
	}

	private static CallIdHeader getCallIdHeader(SipTransactionInfo transactionInfo) throws ParseException, PeerUnavailableException {
		return SipFactory.getInstance().createHeaderFactory().createCallIdHeader(transactionInfo.getCallId());
	}

	private Request createRequest(SipURI requestLine, String method, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List<ViaHeader> viaHeaders, MaxForwardsHeader maxForwards) throws ParseException, PeerUnavailableException {
		return SipFactory.getInstance().createMessageFactory().createRequest(requestLine, method, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
	}

	// -------------------------------------

	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		// sipuri
		SipURI requestURI = createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		List<ViaHeader> viaHeaders = createVia(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);
		// from
		FromHeader fromHeader = createFromHeader(sipConfig.getId(), sipConfig.getDomain(), fromTag);
		// to
		ToHeader toHeader = createToHeader(device.getDeviceId(), device.getHostAddress(), toTag);
		// Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();
		// ceq
		CSeqHeader cSeqHeader = createCSeqHeader(redisCatchStorage.getCSEQ(), Request.MESSAGE);

		Request request = createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		ContentTypeHeader contentTypeHeader = createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, String ssrc, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		//请求行
		SipURI requestLine = getSipURI(device, channelId);
		//via
		List<ViaHeader> viaHeaders = createVia(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);
		//from
		//必须要有标记，否则无法创建会话，无法回应ack
		FromHeader fromHeader = createFromHeader(sipConfig.getId(), sipConfig.getDomain(), fromTag);
		//to
		ToHeader toHeader = createToHeader(channelId, device.getHostAddress(), toTag);
		//Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();

		//ceq
		CSeqHeader cSeqHeader = createCSeqHeader(redisCatchStorage.getCSEQ(), Request.INVITE);

		Request request = createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		Address concatAddress = createAddress(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp()) + ":" + sipConfig.getPort());

		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));
		// Subject
		SubjectHeader subjectHeader = SipFactory.getInstance().createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);
		ContentTypeHeader contentTypeHeader = createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader, String ssrc) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		//请求行
		SipURI requestLine = getSipURI(device, channelId);
		// via
		List<ViaHeader> viaHeaders = createVia(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);

		//from
		//必须要有标记，否则无法创建会话，无法回应ack
		FromHeader fromHeader = createFromHeader(sipConfig.getId(), sipConfig.getDomain(), fromTag);
		//to
		ToHeader toHeader = createToHeader(channelId, device.getHostAddress(), toTag);
		//Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();

		//ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.INVITE);

		Request request = createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = createAddress(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp()) + ":" + sipConfig.getPort());

		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		// Subject
		SubjectHeader subjectHeader = SipFactory.getInstance().createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);

		ContentTypeHeader contentTypeHeader = createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createByteRequest(Device device, String channelId, SipTransactionInfo transactionInfo) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		//请求行
		SipURI requestLine = getSipURI(device, channelId);
		// via
		List<ViaHeader> viaHeaders = createVia(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), SipUtils.getNewViaTag());

		//from
		FromHeader fromHeader = createFromHeader(sipConfig.getId(), sipConfig.getDomain(), transactionInfo.getFromTag());
		//to
		ToHeader toHeader = createToHeader(channelId, device.getHostAddress(), transactionInfo.getToTag());
		//Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();

		//ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.BYE);

		CallIdHeader callIdHeader = getCallIdHeader(transactionInfo);
		Request request = createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		Address concatAddress = createAddress(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp()) + ":" + sipConfig.getPort());
		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		return request;
	}

	public Request createSubscribeRequest(Device device, String content, SIPRequest requestOld, Integer expires, String event, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		// sipuri
		SipURI requestURI = getSipURI(device, device.getDeviceId());
		// via
		List<ViaHeader> viaHeaders = createVia(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), SipUtils.getNewViaTag());

		// from
		FromHeader fromHeader = createFromHeader(sipConfig.getId(), sipConfig.getDomain(), requestOld == null ? SipUtils.getNewFromTag() : requestOld.getFromTag());
		// to
		ToHeader toHeader = createToHeader(device.getDeviceId(), device.getHostAddress(), requestOld == null ? null : requestOld.getToTag());

		// Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();

		// ceq
		CSeqHeader cSeqHeader = createCSeqHeader(redisCatchStorage.getCSEQ(), Request.SUBSCRIBE);

		Request request = createRequest(requestURI, Request.SUBSCRIBE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);


		Address concatAddress = createAddress(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp()) + ":" + sipConfig.getPort());
		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		// Expires
		ExpiresHeader expireHeader = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(expires);
		request.addHeader(expireHeader);

		// Event
		EventHeader eventHeader = SipFactory.getInstance().createHeaderFactory().createEventHeader(event);

		int random = (int) Math.floor(Math.random() * 10000);
		eventHeader.setEventId(random + "");
		request.addHeader(eventHeader);

		ContentTypeHeader contentTypeHeader = createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		return request;
	}

	public SIPRequest createInfoRequest(Device device, String channelId, String content, SipTransactionInfo transactionInfo)
			throws SipException, ParseException, InvalidArgumentException {
		if (device == null || transactionInfo == null) {
			return null;
		}
		//请求行
		SipURI requestLine = getSipURI(device, channelId);
		// via
		List<ViaHeader> viaHeaders = createVia(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), SipUtils.getNewViaTag());

		//from
		FromHeader fromHeader = createFromHeader(sipConfig.getId(), sipConfig.getDomain(), transactionInfo.getFromTag());
		//to
		ToHeader toHeader = createToHeader(channelId, device.getHostAddress(), transactionInfo.getToTag());
		//Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();

		//ceq
		CSeqHeader cSeqHeader = createCSeqHeader(redisCatchStorage.getCSEQ(), Request.INFO);
		CallIdHeader callIdHeader = getCallIdHeader(transactionInfo);
		SIPRequest request = (SIPRequest) createRequest(requestLine, Request.INFO, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);


		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		Address concatAddress = createAddress(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp()) + ":" + sipConfig.getPort());
		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		if (content != null) {
			ContentTypeHeader contentTypeHeader = createContentTypeHeader("Application", "MANSRTSP");
			request.setContent(content, contentTypeHeader);
		}
		return request;
	}

	public Request createAckRequest(String localIp, SipURI sipURI, SIPResponse sipResponse) throws ParseException, InvalidArgumentException, PeerUnavailableException {


		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(localIp, sipConfig.getPort(), sipResponse.getTopmostViaHeader().getTransport(), SipUtils.getNewViaTag());
		viaHeaders.add(viaHeader);

		//Forwards
		MaxForwardsHeader maxForwards = getForwardsHeader();

		//ceq
		CSeqHeader cSeqHeader = createCSeqHeader(sipResponse.getCSeqHeader().getSeqNumber(), Request.ACK);

		Request request = createRequest(sipURI, Request.ACK, sipResponse.getCallIdHeader(), cSeqHeader, sipResponse.getFromHeader(), sipResponse.getToHeader(), viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		Address concatAddress = createAddress(sipConfig.getId(), localIp + ":" + sipConfig.getPort());

		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		return request;
	}
}
