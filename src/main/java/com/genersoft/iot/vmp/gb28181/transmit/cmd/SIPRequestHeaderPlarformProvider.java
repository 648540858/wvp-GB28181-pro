package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @description: 平台命令request创造器 TODO 冗余代码太多待优化
 * @author: panll
 * @date: 2020年5月6日 上午9:29:02
 */
@Component
public class SIPRequestHeaderPlarformProvider {

	@Autowired
	private SipConfig sipConfig;
	
	@Autowired
	private SipFactory sipFactory;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;


	public Request createKeetpaliveMessageRequest(ParentPlatform parentPlatform, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP() + ":" + parentPlatform.getServerPort());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getIp(), sipConfig.getPort(),
				parentPlatform.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(),
				sipConfig.getIp() + ":" + sipConfig.getPort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP() + ":" + parentPlatform.getServerPort() );
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);


		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(Request.MESSAGE), Request.MESSAGE);

		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		List<String> agentParam = new ArrayList<>();
		agentParam.add("wvp-pro");
		UserAgentHeader userAgentHeader = sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
		request.addHeader(userAgentHeader);

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}


	public Request createRegisterRequest(@NotNull ParentPlatform platform, long CSeq, String fromTag, String viaTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		String sipAddress = sipConfig.getIp() + ":" + sipConfig.getPort();
		//请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(platform.getServerGBId(),
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

		List<String> agentParam = new ArrayList<>();
		agentParam.add("wvp-pro");
		UserAgentHeader userAgentHeader = sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
		request.addHeader(userAgentHeader);

		return request;
	}

	public Request createRegisterRequest(@NotNull ParentPlatform parentPlatform, String fromTag, String viaTag,
										 String callId, WWWAuthenticateHeader www , CallIdHeader callIdHeader) throws ParseException, PeerUnavailableException, InvalidArgumentException {


		Request registerRequest = createRegisterRequest(parentPlatform, redisCatchStorage.getCSEQ(Request.REGISTER), fromTag, viaTag, callIdHeader);
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP() + ":" + parentPlatform.getServerPort());
		if (www == null) {
			AuthorizationHeader authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader("Digest");
			authorizationHeader.setUsername(parentPlatform.getDeviceGBId());
			authorizationHeader.setURI(requestURI);
			authorizationHeader.setAlgorithm("MD5");
			registerRequest.addHeader(authorizationHeader);
			return  registerRequest;
		}
		String realm = www.getRealm();
		String nonce = www.getNonce();
		String scheme = www.getScheme();

		// 参考 https://blog.csdn.net/y673533511/article/details/88388138
		// qop 保护质量 包含auth（默认的）和auth-int（增加了报文完整性检测）两种策略
		String qop = www.getQop();

		callIdHeader.setCallId(callId);

		String cNonce = null;
		String nc = "00000001";
		if (qop != null) {
			if ("auth".equals(qop)) {
				// 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
				// 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
				cNonce = UUID.randomUUID().toString();

			}else if ("auth-int".equals(qop)){
				// TODO
			}
		}
		String HA1 = DigestUtils.md5DigestAsHex((parentPlatform.getDeviceGBId() + ":" + realm + ":" + parentPlatform.getPassword()).getBytes());
		String HA2=DigestUtils.md5DigestAsHex((Request.REGISTER + ":" + requestURI.toString()).getBytes());

		StringBuffer reStr = new StringBuffer(200);
		reStr.append(HA1);
		reStr.append(":");
		reStr.append(nonce);
		reStr.append(":");
		if (qop != null) {
			reStr.append(nc);
			reStr.append(":");
			reStr.append(cNonce);
			reStr.append(":");
			reStr.append(qop);
			reStr.append(":");
		}
		reStr.append(HA2);

		String RESPONSE = DigestUtils.md5DigestAsHex(reStr.toString().getBytes());

		AuthorizationHeader authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader(scheme);
		authorizationHeader.setUsername(parentPlatform.getDeviceGBId());
		authorizationHeader.setRealm(realm);
		authorizationHeader.setNonce(nonce);
		authorizationHeader.setURI(requestURI);
		authorizationHeader.setResponse(RESPONSE);
		authorizationHeader.setAlgorithm("MD5");
		if (qop != null) {
			authorizationHeader.setQop(qop);
			authorizationHeader.setCNonce(cNonce);
			authorizationHeader.setNonceCount(1);
		}
		registerRequest.addHeader(authorizationHeader);

		return registerRequest;
	}


	public Request createMessageRequest(ParentPlatform parentPlatform, String content, String fromTag, CallIdHeader callIdHeader) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP()+ ":" + parentPlatform.getServerPort());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(parentPlatform.getDeviceIp(), Integer.parseInt(parentPlatform.getDevicePort()),
				parentPlatform.getTransport(), null);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(),
				parentPlatform.getDeviceIp() + ":" + parentPlatform.getDevicePort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerGBDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, null);

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(Request.MESSAGE), Request.MESSAGE);
		MessageFactoryImpl messageFactory = (MessageFactoryImpl) sipFactory.createMessageFactory();
		// 设置编码， 防止中文乱码
		messageFactory.setDefaultContentEncodingCharset("gb2312");
		request = messageFactory.createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);
		List<String> agentParam = new ArrayList<>();
		agentParam.add("wvp-pro");
		UserAgentHeader userAgentHeader = sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
		request.addHeader(userAgentHeader);

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}

    public Request createNotifyRequest(ParentPlatform parentPlatform, String content, CallIdHeader callIdHeader, String viaTag, SubscribeInfo subscribeInfo) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP()+ ":" + parentPlatform.getServerPort());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(parentPlatform.getDeviceIp(), Integer.parseInt(parentPlatform.getDevicePort()),
				parentPlatform.getTransport(), subscribeInfo.getBranch());
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(),
				parentPlatform.getDeviceIp() + ":" + parentPlatform.getDevicePort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, subscribeInfo.getToTag());
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerGBDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, subscribeInfo.getFromTag());

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(Request.NOTIFY), Request.NOTIFY);
		MessageFactoryImpl messageFactory = (MessageFactoryImpl) sipFactory.createMessageFactory();
		// 设置编码， 防止中文乱码
		messageFactory.setDefaultContentEncodingCharset("gb2312");
		request = messageFactory.createRequest(requestURI, Request.NOTIFY, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);
		List<String> agentParam = new ArrayList<>();
		agentParam.add("wvp-pro");
		UserAgentHeader userAgentHeader = sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
		request.addHeader(userAgentHeader);

		EventHeader event = sipFactory.createHeaderFactory().createEventHeader(subscribeInfo.getEventType());
		if (subscribeInfo.getEventId() != null) {
			event.setEventId(subscribeInfo.getEventId());
		}

		request.addHeader(event);

		SubscriptionStateHeader active = sipFactory.createHeaderFactory().createSubscriptionStateHeader("active");
		request.setHeader(active);

		String sipAddress = sipConfig.getIp() + ":" + sipConfig.getPort();
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory()
				.createSipURI(parentPlatform.getDeviceGBId(), sipAddress));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
    }
}
