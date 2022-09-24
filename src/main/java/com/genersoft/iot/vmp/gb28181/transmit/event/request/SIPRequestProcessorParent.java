package com.genersoft.iot.vmp.gb28181.transmit.event.request;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import org.apache.commons.lang3.ArrayUtils;
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
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
		String transport = reqViaHeader.getTransport();
		boolean isTcp = "TCP".equals(transport);

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
			}finally {

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

	class ResponseAckExtraParam{
		String content;
		ContentTypeHeader contentTypeHeader;
		SipURI sipURI;
		int expires = -1;
	}

	/***
	 * 回复状态码
	 * 100 trying
	 * 200 OK
	 * 400
	 * 404
	 */
	public SIPResponse responseAck(ServerTransaction serverTransaction, int statusCode) throws SipException, InvalidArgumentException, ParseException {
		return responseAck(serverTransaction, statusCode, null);
	}

	public SIPResponse responseAck(ServerTransaction serverTransaction, int statusCode, String msg) throws SipException, InvalidArgumentException, ParseException {
		return responseAck(serverTransaction, statusCode, msg, null);
	}

	public SIPResponse responseAck(ServerTransaction serverTransaction, int statusCode, String msg, ResponseAckExtraParam responseAckExtraParam) throws SipException, InvalidArgumentException, ParseException {
		ToHeader toHeader = (ToHeader) serverTransaction.getRequest().getHeader(ToHeader.NAME);
		if (toHeader.getTag() == null) {
			toHeader.setTag(SipUtils.getNewTag());
		}
		SIPResponse response = (SIPResponse)getMessageFactory().createResponse(statusCode, serverTransaction.getRequest());
		if (msg != null) {
			response.setReasonPhrase(msg);
		}
		if (responseAckExtraParam != null) {
			if (responseAckExtraParam.sipURI != null && serverTransaction.getRequest().getMethod().equals(Request.INVITE)) {
				logger.debug("responseSdpAck SipURI: {}:{}", responseAckExtraParam.sipURI.getHost(), responseAckExtraParam.sipURI.getPort());
				Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(
						SipFactory.getInstance().createAddressFactory().createSipURI(responseAckExtraParam.sipURI.getUser(),  responseAckExtraParam.sipURI.getHost()+":"+responseAckExtraParam.sipURI.getPort()
						));
				response.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));
			}
			if (responseAckExtraParam.contentTypeHeader != null) {
				response.setContent(responseAckExtraParam.content, responseAckExtraParam.contentTypeHeader);
			}

			if (serverTransaction.getRequest().getMethod().equals(Request.SUBSCRIBE)) {
				if (responseAckExtraParam.expires == -1) {
					logger.error("[参数不全] 2xx的SUBSCRIBE回复，必须设置Expires header");
				}else {
					ExpiresHeader expiresHeader = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(responseAckExtraParam.expires);
					response.addHeader(expiresHeader);
				}
			}
		}else {
			if (serverTransaction.getRequest().getMethod().equals(Request.SUBSCRIBE)) {
				logger.error("[参数不全] 2xx的SUBSCRIBE回复，必须设置Expires header");
			}
		}
		serverTransaction.sendResponse(response);
		if (statusCode >= 200 && !"NOTIFY".equalsIgnoreCase(serverTransaction.getRequest().getMethod())) {
			if (serverTransaction.getDialog() != null) {
				serverTransaction.getDialog().delete();
			}
		}
		return response;
	}

	/**
	 * 回复带sdp的200
	 */
	public SIPResponse responseSdpAck(ServerTransaction serverTransaction, String sdp, ParentPlatform platform) throws SipException, InvalidArgumentException, ParseException {

		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");

		// 兼容国标中的使用编码@域名作为RequestURI的情况
		SipURI sipURI = (SipURI)serverTransaction.getRequest().getRequestURI();
		if (sipURI.getPort() == -1) {
			sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(),  platform.getServerIP()+":"+platform.getServerPort());
		}
		ResponseAckExtraParam responseAckExtraParam = new ResponseAckExtraParam();
		responseAckExtraParam.contentTypeHeader = contentTypeHeader;
		responseAckExtraParam.content = sdp;
		responseAckExtraParam.sipURI = sipURI;

		return responseAck(serverTransaction, Response.OK, null, responseAckExtraParam);
	}

	/**
	 * 回复带xml的200
	 */
	public SIPResponse responseXmlAck(ServerTransaction serverTransaction, String xml, ParentPlatform platform, Integer expires) throws SipException, InvalidArgumentException, ParseException {
		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");

		SipURI sipURI = (SipURI)serverTransaction.getRequest().getRequestURI();
		if (sipURI.getPort() == -1) {
			sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(),  platform.getServerIP()+":"+platform.getServerPort());
		}
		ResponseAckExtraParam responseAckExtraParam = new ResponseAckExtraParam();
		responseAckExtraParam.contentTypeHeader = contentTypeHeader;
		responseAckExtraParam.content = xml;
		responseAckExtraParam.sipURI = sipURI;
		responseAckExtraParam.expires = expires;
		return responseAck(serverTransaction, Response.OK, null, responseAckExtraParam);
	}

	public Element getRootElement(RequestEvent evt) throws DocumentException {
		return getRootElement(evt, "gb2312");
	}
	public Element getRootElement(RequestEvent evt, String charset) throws DocumentException {
		if (charset == null) {
			charset = "gb2312";
		}
		Request request = evt.getRequest();
		SAXReader reader = new SAXReader();
		reader.setEncoding(charset);
		// 对海康出现的未转义字符做处理。
		String[] destStrArray = new String[]{"&lt;","&gt;","&amp;","&apos;","&quot;"};
		char despChar = '&'; // 或许可扩展兼容其他字符
		byte destBye = (byte) despChar;
		List<Byte> result = new ArrayList<>();
		byte[] rawContent = request.getRawContent();
		if (rawContent == null) {
			return null;
		}
		for (int i = 0; i < rawContent.length; i++) {
			if (rawContent[i] == destBye) {
				boolean resul = false;
				for (String destStr : destStrArray) {
					if (i + destStr.length() <= rawContent.length) {
						byte[] bytes = Arrays.copyOfRange(rawContent, i, i + destStr.length());
						resul = resul || (Arrays.equals(bytes,destStr.getBytes()));
					}
				}
				if (resul) {
					result.add(rawContent[i]);
				}
			}else {
				result.add(rawContent[i]);
			}
		}
		Byte[] bytes = new Byte[0];
		byte[] bytesResult = ArrayUtils.toPrimitive(result.toArray(bytes));

		Document xml = reader.read(new ByteArrayInputStream(bytesResult));
		return xml.getRootElement();
	}

}
