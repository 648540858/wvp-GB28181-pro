package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.play.bean.PlayResult;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Vector;

/**    
 * @Description:处理INVITE请求
 * @author: panll
 * @date:   2021年1月14日
 */
@SuppressWarnings("rawtypes")
public class InviteRequestProcessor extends SIPRequestAbstractProcessor {

	private final static Logger logger = LoggerFactory.getLogger(MessageRequestProcessor.class);

	private SIPCommanderFroPlatform cmderFroPlatform;

	private IVideoManagerStorager storager;

	private IRedisCatchStorage  redisCatchStorage;

	private SIPCommander cmder;

	private IPlayService playService;

	private ZLMRTPServerFactory zlmrtpServerFactory;

	public ZLMRTPServerFactory getZlmrtpServerFactory() {
		return zlmrtpServerFactory;
	}

	public void setZlmrtpServerFactory(ZLMRTPServerFactory zlmrtpServerFactory) {
		this.zlmrtpServerFactory = zlmrtpServerFactory;
	}

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

			FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
			AddressImpl address = (AddressImpl) fromHeader.getAddress();
			SipUri uri = (SipUri) address.getURI();
			platformId = uri.getUser();

			if (platformId == null || channelId == null) {
				logger.info("无法从FromHeader的Address中获取到平台id，返回404");
				responseAck(evt, Response.BAD_REQUEST); // 参数不全， 发400，请求错误
				return;
			}
			// 查询平台下是否有该通道
			DeviceChannel channel = storager.queryChannelInParentPlatform(platformId, channelId);
			if (channel == null) {
				logger.info("通道不存在，返回404");
				responseAck(evt, Response.NOT_FOUND); // 通道不存在，发404，资源不存在
				return;
			}else {
				responseAck(evt, Response.TRYING); // 通道存在，发100，trying
			}
			// 解析sdp消息, 使用jainsip 自带的sdp解析方式
			String contentString = new String(request.getRawContent());

			// jainSip不支持y=字段， 移除移除以解析。
			int ssrcIndex = contentString.indexOf("y=");
			String ssrc = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
			//ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段
			// String ssrc = contentString.substring(ssrcIndex + 2, contentString.length())
			// 		.replace("\r\n", "").replace("\n", "");

			String substring = contentString.substring(0, contentString.indexOf("y="));
			SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);

			//  获取支持的格式
			Vector mediaDescriptions = sdp.getMediaDescriptions(true);
			// 查看是否支持PS 负载96
			//String ip = null;
			int port = -1;
			//boolean recvonly = false;
			boolean mediaTransmissionTCP = false;
			Boolean tcpActive = null;
			for (int i = 0; i < mediaDescriptions.size(); i++) {
				MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(i);
				Media media = mediaDescription.getMedia();

				Vector mediaFormats = media.getMediaFormats(false);
				if (mediaFormats.contains("96")) {
					port = media.getMediaPort();
					//String mediaType = media.getMediaType();
					String protocol = media.getProtocol();

					// 区分TCP发流还是udp， 当前默认udp
					if ("TCP/RTP/AVP".equals(protocol)) {
						String setup = mediaDescription.getAttribute("setup");
						if (setup != null) {
							mediaTransmissionTCP = true;
							if ("active".equals(setup)) {
								tcpActive = true;
							}else if ("passive".equals(setup)) {
								tcpActive = false;
							}
						}
					}
					break;
				}
			}
			if (port == -1) {
				logger.info("不支持的媒体格式，返回415");
				// 回复不支持的格式
				responseAck(evt, Response.UNSUPPORTED_MEDIA_TYPE); // 不支持的格式，发415
				return;
			}
			String username = sdp.getOrigin().getUsername();
			String addressStr = sdp.getOrigin().getAddress();
			//String sessionName = sdp.getSessionName().getValue();
			logger.info("[上级点播]用户：{}， 地址：{}:{}， ssrc：{}", username, addressStr, port, ssrc);

			Device device = storager.queryVideoDeviceByPlatformIdAndChannelId(platformId, channelId);
			if (device == null) {
				logger.warn("点播平台{}的通道{}时未找到设备信息", platformId, channel);
				responseAck(evt, Response.SERVER_INTERNAL_ERROR);
				return;
			}
			SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(addressStr, port, ssrc, platformId, device.getDeviceId(), channelId,
					mediaTransmissionTCP);
			if (tcpActive != null) {
				sendRtpItem.setTcpActive(tcpActive);
			}
			if (sendRtpItem == null) {
				logger.warn("服务器端口资源不足");
				responseAck(evt, Response.BUSY_HERE);
				return;
			}

			// 写入redis， 超时时回复
			redisCatchStorage.updateSendRTPSever(sendRtpItem);
			// 通知下级推流，
			PlayResult playResult = playService.play(device.getDeviceId(), channelId, (responseJSON)->{
				// 收到推流， 回复200OK, 等待ack
				sendRtpItem.setStatus(1);
				redisCatchStorage.updateSendRTPSever(sendRtpItem);
				// TODO 添加对tcp的支持
				MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
				StringBuffer content = new StringBuffer(200);
				content.append("v=0\r\n");
				content.append("o="+"00000"+" 0 0 IN IP4 "+mediaInfo.getWanIp()+"\r\n");
				content.append("s=Play\r\n");
				content.append("c=IN IP4 "+mediaInfo.getWanIp()+"\r\n");
				content.append("t=0 0\r\n");
				content.append("m=video "+ sendRtpItem.getLocalPort()+" RTP/AVP 96\r\n");
				content.append("a=sendonly\r\n");
				content.append("a=rtpmap:96 PS/90000\r\n");
				content.append("y="+ ssrc + "\r\n");
				content.append("f=\r\n");

				try {
					responseAck(evt, content.toString());
				} catch (SipException e) {
					e.printStackTrace();
				} catch (InvalidArgumentException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			},(event -> {
				// 未知错误。直接转发设备点播的错误
				Response response = null;
				try {
					response = getMessageFactory().createResponse(event.getResponse().getStatusCode(), evt.getRequest());
					getServerTransaction(evt).sendResponse(response);

				} catch (ParseException | SipException | InvalidArgumentException e) {
					e.printStackTrace();
				}
			}));
			if (logger.isDebugEnabled()) {
				logger.debug(playResult.getResult().toString());
			}

		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
			logger.warn("sdp解析错误");
			e.printStackTrace();
		} catch (SdpParseException e) {
			e.printStackTrace();
		} catch (SdpException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 回复状态码
	 * 100 trying
	 * 200 OK
	 * 400
	 * 404
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void responseAck(RequestEvent evt, int statusCode) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(statusCode, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	/**
	 * 回复带sdp的200
	 * @param evt
	 * @param sdp
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void responseAck(RequestEvent evt, String sdp) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		SipFactory sipFactory = SipFactory.getInstance();
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
		response.setContent(sdp, contentTypeHeader);

		SipURI sipURI = (SipURI)evt.getRequest().getRequestURI();

		Address concatAddress = sipFactory.createAddressFactory().createAddress(
				sipFactory.createAddressFactory().createSipURI(sipURI.getUser(),  sipURI.getHost()+":"+sipURI.getPort()
				));
		response.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
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

	public SIPCommander getCmder() {
		return cmder;
	}

	public void setCmder(SIPCommander cmder) {
		this.cmder = cmder;
	}

	public IPlayService getPlayService() {
		return playService;
	}

	public void setPlayService(IPlayService playService) {
		this.playService = playService;
	}

	public IRedisCatchStorage getRedisCatchStorage() {
		return redisCatchStorage;
	}

	public void setRedisCatchStorage(IRedisCatchStorage redisCatchStorage) {
		this.redisCatchStorage = redisCatchStorage;
	}
}
