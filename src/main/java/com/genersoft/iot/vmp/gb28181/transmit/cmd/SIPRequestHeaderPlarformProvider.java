package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.util.ArrayList;
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
	private SipLayer sipLayer;

	@Autowired
	private GitUtil gitUtil;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	public Request createRegisterRequest(@NotNull ParentPlatform parentPlatform, long CSeq, String fromTag, String toTag, CallIdHeader callIdHeader, int expires) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		String sipAddress = parentPlatform.getDeviceIp() + ":" + parentPlatform.getDevicePort();
		//请求行
		SipURI requestLine = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getServerGBId(),
				parentPlatform.getServerIP() + ":" + parentPlatform.getServerPort());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(parentPlatform.getDeviceIp(),
				Integer.parseInt(parentPlatform.getDevicePort()), parentPlatform.getTransport(), SipUtils.getNewViaTag());
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(), sipConfig.getDomain());
		Address fromAddress = SipFactory.getInstance().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, fromTag);
		//to
		SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(), sipConfig.getDomain());
		Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress,toTag);

		//Forwards
		MaxForwardsHeader maxForwards = SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(CSeq, Request.REGISTER);
		request = SipFactory.getInstance().createMessageFactory().createRequest(requestLine, Request.REGISTER, callIdHeader,
				cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(SipFactory.getInstance().createAddressFactory()
				.createSipURI(parentPlatform.getDeviceGBId(), sipAddress));
		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		ExpiresHeader expiresHeader = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(expires);
		request.addHeader(expiresHeader);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		return request;
	}

	public Request createRegisterRequest(@NotNull ParentPlatform parentPlatform, String fromTag, String toTag,
										 WWWAuthenticateHeader www , CallIdHeader callIdHeader, int expires) throws ParseException, PeerUnavailableException, InvalidArgumentException {


		Request registerRequest = createRegisterRequest(parentPlatform, redisCatchStorage.getCSEQ(), fromTag, toTag, callIdHeader, expires);
		SipURI requestURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP() + ":" + parentPlatform.getServerPort());
		if (www == null) {
			AuthorizationHeader authorizationHeader = SipFactory.getInstance().createHeaderFactory().createAuthorizationHeader("Digest");
			String username = parentPlatform.getUsername();
			if ( username == null || username == "" )
			{
				authorizationHeader.setUsername(parentPlatform.getDeviceGBId());
			} else {
				authorizationHeader.setUsername(username);
			}
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

		String cNonce = null;
		String nc = "00000001";
		if (qop != null) {
			if ("auth".equalsIgnoreCase(qop)) {
				// 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
				// 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
				cNonce = UUID.randomUUID().toString();

			}else if ("auth-int".equalsIgnoreCase(qop)){
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

		AuthorizationHeader authorizationHeader = SipFactory.getInstance().createHeaderFactory().createAuthorizationHeader(scheme);
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

	public Request createMessageRequest(ParentPlatform parentPlatform, String content, SendRtpItem sendRtpItem) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		CallIdHeader callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(sendRtpItem.getCallId());
		callIdHeader.setCallId(sendRtpItem.getCallId());
		return createMessageRequest(parentPlatform, content, sendRtpItem.getToTag(), SipUtils.getNewViaTag(), sendRtpItem.getFromTag(), callIdHeader);
	}

	public Request createMessageRequest(ParentPlatform parentPlatform, String content, String fromTag, String viaTag, CallIdHeader callIdHeader) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		return createMessageRequest(parentPlatform, content, fromTag, viaTag, null, callIdHeader);
	}


	public Request createMessageRequest(ParentPlatform parentPlatform, String content, String fromTag, String viaTag, String toTag, CallIdHeader callIdHeader) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		Request request = null;
		String serverAddress = parentPlatform.getServerIP()+ ":" + parentPlatform.getServerPort();
		// sipuri
		SipURI requestURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getServerGBId(), serverAddress);
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(parentPlatform.getDeviceIp(), Integer.parseInt(parentPlatform.getDevicePort()),
				parentPlatform.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		// SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(), parentPlatform.getDeviceIp() + ":" + parentPlatform.getDeviceIp());
		SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(), sipConfig.getDomain());
		Address fromAddress = SipFactory.getInstance().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getServerGBId(), serverAddress);
		Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress, toTag);

		// Forwards
		MaxForwardsHeader maxForwards = SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.MESSAGE);
		MessageFactoryImpl messageFactory = (MessageFactoryImpl) SipFactory.getInstance().createMessageFactory();
		// 设置编码， 防止中文乱码
		messageFactory.setDefaultContentEncodingCharset(parentPlatform.getCharacterSet());
		request = messageFactory.createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public SIPRequest createNotifyRequest(ParentPlatform parentPlatform, String content, SubscribeInfo subscribeInfo) throws PeerUnavailableException, ParseException, InvalidArgumentException {
		SIPRequest request = null;
		// sipuri
		SipURI requestURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerIP()+ ":" + parentPlatform.getServerPort());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(parentPlatform.getDeviceIp(), Integer.parseInt(parentPlatform.getDevicePort()),
				parentPlatform.getTransport(), SipUtils.getNewViaTag());
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getDeviceGBId(),
				parentPlatform.getDeviceIp() + ":" + parentPlatform.getDevicePort());
		Address fromAddress = SipFactory.getInstance().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, subscribeInfo.getResponse() != null ? subscribeInfo.getResponse().getToTag(): subscribeInfo.getSimulatedToTag());
		// to
		SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(parentPlatform.getServerGBId(), parentPlatform.getServerGBDomain());
		Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress, subscribeInfo.getRequest() != null ?subscribeInfo.getRequest().getFromTag(): subscribeInfo.getSimulatedFromTag());

		// Forwards
		MaxForwardsHeader maxForwards = SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.NOTIFY);
		MessageFactoryImpl messageFactory = (MessageFactoryImpl) SipFactory.getInstance().createMessageFactory();
		// 设置编码， 防止中文乱码
		messageFactory.setDefaultContentEncodingCharset("gb2312");

		CallIdHeader callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(subscribeInfo.getRequest() != null ? subscribeInfo.getRequest().getCallIdHeader().getCallId(): subscribeInfo.getSimulatedCallId());

		request = (SIPRequest) messageFactory.createRequest(requestURI, Request.NOTIFY, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		EventHeader event = SipFactory.getInstance().createHeaderFactory().createEventHeader(subscribeInfo.getEventType());
		if (subscribeInfo.getEventId() != null) {
			event.setEventId(subscribeInfo.getEventId());
		}

		request.addHeader(event);

		SubscriptionStateHeader active = SipFactory.getInstance().createHeaderFactory().createSubscriptionStateHeader("active");
		request.setHeader(active);

		String sipAddress = parentPlatform.getDeviceIp() + ":" + parentPlatform.getDevicePort();
		Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(SipFactory.getInstance().createAddressFactory()
				.createSipURI(parentPlatform.getDeviceGBId(), sipAddress));
		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
    }

	public SIPRequest createByeRequest(ParentPlatform platform, SendRtpItem sendRtpItem) throws PeerUnavailableException, ParseException, InvalidArgumentException {

		if (sendRtpItem == null ) {
			return null;
		}

		SIPRequest request = null;
		// sipuri
		SipURI requestURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(), platform.getServerIP()+ ":" + platform.getServerPort());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(platform.getDeviceIp(), Integer.parseInt(platform.getDevicePort()),
				platform.getTransport(), SipUtils.getNewViaTag());
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(sendRtpItem.getChannelId(),
				platform.getDeviceIp() + ":" + platform.getDevicePort());
		Address fromAddress = SipFactory.getInstance().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, sendRtpItem.getToTag());
		// to
		SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(), platform.getServerGBDomain());
		Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress, sendRtpItem.getFromTag());

		// Forwards
		MaxForwardsHeader maxForwards = SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(redisCatchStorage.getCSEQ(), Request.BYE);

		CallIdHeader callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(sendRtpItem.getCallId());

		request = (SIPRequest) SipFactory.getInstance().createMessageFactory().createRequest(requestURI, Request.BYE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader(gitUtil));

		String sipAddress = platform.getDeviceIp() + ":" + platform.getDevicePort();
		Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(SipFactory.getInstance().createAddressFactory()
				.createSipURI(platform.getDeviceGBId(), sipAddress));

		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		return request;
	}
}
