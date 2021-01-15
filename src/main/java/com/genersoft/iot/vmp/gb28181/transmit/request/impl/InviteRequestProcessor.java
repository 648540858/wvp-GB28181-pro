package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.sdp.Codec;
import com.genersoft.iot.vmp.gb28181.sdp.MediaDescription;
import com.genersoft.iot.vmp.gb28181.sdp.SdpParser;
import com.genersoft.iot.vmp.gb28181.sdp.SessionDescription;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**    
 * @Description:处理INVITE请求
 * @author: panll
 * @date:   2021年1月14日
 */
public class InviteRequestProcessor extends SIPRequestAbstractProcessor {

	private final static Logger logger = LoggerFactory.getLogger(MessageRequestProcessor.class);

	private SIPCommanderFroPlatform cmderFroPlatform;

	private IVideoManagerStorager storager;

	/**
	 * 处理invite请求
	 * 
	 * @param evt
	 *            请求消息
	 */ 
	@Override
	public void process(RequestEvent evt) {
		//  Invite Request消息实现，此消息一般为级联消息，上级给下级发送请求视频指令
		try {
			Request request = evt.getRequest();
			SipURI sipURI = (SipURI) request.getRequestURI();
			String channelId = sipURI.getUser();
			String platformId = null;
//			SubjectHeader subjectHeader = (SubjectHeader)request.getHeader(SubjectHeader.NAME);
//			// 查询通道是否存在 不存在回复404
//			if (subjectHeader != null) { // 存在则从subjectHeader 获取平台信息
//				String subject = subjectHeader.getSubject();
//				if (subject != null) {
//					String[] info1 = subject.split(",");
//					if (info1 != null && info1 .length == 2) {
//						String[] info2 = info1[1].split(":");
//						if (info2 != null && info2.length == 2) {
//							platformId = info2[0];
//						}
//					}
//				}
//			}

			FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
			AddressImpl address = (AddressImpl) fromHeader.getAddress();
			SipUri uri = (SipUri) address.getURI();
			platformId = uri.getUser();

//			if (platformId == null) { // 不存在则从fromHeader 获取平台信息
//				FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
//				platformId = fromHeader.getName();
//			}
			if (platformId == null || channelId == null) {
				response400Ack(evt); // 参数不全， 发400，请求错误
				return;
			}
			// 查询平台下是否有该通道
			DeviceChannel channel = storager.queryChannelInParentPlatform(platformId, channelId);
			if (channel == null) {
				response404Ack(evt); // 通道不存在，发404，资源不存在
				return;
			}else {
				response100Ack(evt); // 通道存在，发100，trying
			}
			// 解析sdp消息
			byte[] sdpByteArray = request.getRawContent();
			SdpParser sdpParser = new SdpParser(); // TODO keng
			SessionDescription sdp = sdpParser.parse(sdpByteArray);
			//  获取支持的格式
			List<MediaDescription> mediaDescriptions = sdp.getMediaDescriptions();
			// 查看是否支持PS 负载96
			String ip = null;
			int port = -1;
			for (MediaDescription mediaDescription : mediaDescriptions) {

				List<Codec> codecs = mediaDescription.getCodecs();
				for (Codec codec : codecs) {
					if("96".equals(codec.getPayloadType()) || "PS".equals(codec.getName()) || "ps".equals(codec.getName())) {
						ip = mediaDescription.getIpAddress().getHostName();
						port = mediaDescription.getPort();
						break;
					}
				}
			}
			if (ip == null || port == -1) { // TODO 没有合适的视频流格式， 可配置是否使用第一个media信息
				if (mediaDescriptions.size() > 0) {
					ip = mediaDescriptions.get(0).getIpAddress().getHostName();
					port = mediaDescriptions.get(0).getPort();
				}
			}

			if (ip == null || port == -1) {
				response488Ack(evt);
				return;
			}


			String ssrc = sdp.getSsrc();
			// 通知下级推流，
			// 查找合适的端口推流，
			// 发送 200ok
			// 收到ack后调用推流接口




		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			logger.warn("sdp解析错误");
			e.printStackTrace();
		}

	}

	/***
	 * 回复100 trying
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void response100Ack(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.TRYING, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	/***
	 * 回复404
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void response404Ack(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.NOT_FOUND, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	/***
	 * 回复400
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void response400Ack(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.BAD_REQUEST, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	/***
	 * 回复488
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void response488Ack(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.NOT_ACCEPTABLE_HERE, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	/***
	 * 回复200 OK
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void responseAck(RequestEvent evt, String sdp) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		ContentTypeHeader contentTypeHeader = getHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		response.setContent(sdp, contentTypeHeader);
		getServerTransaction(evt).sendResponse(response);
	}










	public SIPCommanderFroPlatform getCmderFroPlatform() {
		return cmderFroPlatform;
	}

	public void setCmderFroPlatform(SIPCommanderFroPlatform cmderFroPlatform) {
		this.cmderFroPlatform = cmderFroPlatform;
	}

	public IVideoManagerStorager getStorager() {
		return storager;
	}

	public void setStorager(IVideoManagerStorager storager) {
		this.storager = storager;
	}
}
