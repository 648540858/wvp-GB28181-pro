package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.SerializeUtils;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * SIP命令类型： INVITE请求
 */
@SuppressWarnings("rawtypes")
@Component
public class InviteRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final static Logger logger = LoggerFactory.getLogger(InviteRequestProcessor.class);

	private String method = "INVITE";

	@Autowired
	private SIPCommanderFroPlatform cmderFroPlatform;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IRedisCatchStorage  redisCatchStorage;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private ISIPCommander commander;

	@Autowired
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private VideoStreamSessionManager sessionManager;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private ZLMMediaListManager mediaListManager;


	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
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
			//从subject读取channelId,不再从request-line读取。 有些平台request-line是平台国标编码，不是设备国标编码。
			//String channelId = sipURI.getUser();
			String channelId = SipUtils.getChannelIdFromHeader(request);
			String requesterId = SipUtils.getUserIdFromFromHeader(request);
			CallIdHeader callIdHeader = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
			if (requesterId == null || channelId == null) {
				logger.info("无法从FromHeader的Address中获取到平台id，返回400");
				responseAck(evt, Response.BAD_REQUEST); // 参数不全， 发400，请求错误
				return;
			}

			// 查询请求是否来自上级平台\设备
			ParentPlatform platform = storager.queryParentPlatByServerGBId(requesterId);
			if (platform == null) {
				inviteFromDeviceHandle(evt, requesterId);
			}else {
				// 查询平台下是否有该通道
				DeviceChannel channel = storager.queryChannelInParentPlatform(requesterId, channelId);
				GbStream gbStream = storager.queryStreamInParentPlatform(requesterId, channelId);
				PlatformCatalog catalog = storager.getCatalog(channelId);
				MediaServerItem mediaServerItem = null;
				// 不是通道可能是直播流
				if (channel != null && gbStream == null ) {
					if (channel.getStatus() == 0) {
						logger.info("通道离线，返回400");
						responseAck(evt, Response.BAD_REQUEST, "channel [" + channel.getChannelId() + "] offline");
						return;
					}
					responseAck(evt, Response.CALL_IS_BEING_FORWARDED); // 通道存在，发181，呼叫转接中
				}else if(channel == null && gbStream != null){
					String mediaServerId = gbStream.getMediaServerId();
					mediaServerItem = mediaServerService.getOne(mediaServerId);
					if (mediaServerItem == null) {
						logger.info("[ app={}, stream={} ]找不到zlm {}，返回410",gbStream.getApp(), gbStream.getStream(), mediaServerId);
						responseAck(evt, Response.GONE);
						return;
					}
					responseAck(evt, Response.CALL_IS_BEING_FORWARDED); // 通道存在，发181，呼叫转接中
				}else if (catalog != null) {
					responseAck(evt, Response.BAD_REQUEST, "catalog channel can not play"); // 目录不支持点播
					return;
				} else {
					logger.info("通道不存在，返回404");
					responseAck(evt, Response.NOT_FOUND); // 通道不存在，发404，资源不存在
					return;
				}
				// 解析sdp消息, 使用jainsip 自带的sdp解析方式
				String contentString = new String(request.getRawContent());

				// jainSip不支持y=字段， 移除以解析。
				int ssrcIndex = contentString.indexOf("y=");
				// 检查是否有y字段
				String ssrcDefault = "0000000000";
				String ssrc;
				SessionDescription sdp;
				if (ssrcIndex >= 0) {
					//ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段
					ssrc = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
					String substring = contentString.substring(0, contentString.indexOf("y="));
					sdp = SdpFactory.getInstance().createSessionDescription(substring);
				}else {
					ssrc = ssrcDefault;
					sdp = SdpFactory.getInstance().createSessionDescription(contentString);
				}
				String sessionName = sdp.getSessionName().getValue();

				Long startTime = null;
				Long stopTime = null;
				Date start = null;
				Date end = null;
				if (sdp.getTimeDescriptions(false) != null && sdp.getTimeDescriptions(false).size() > 0) {
					TimeDescriptionImpl timeDescription = (TimeDescriptionImpl)(sdp.getTimeDescriptions(false).get(0));
					TimeField startTimeFiled = (TimeField)timeDescription.getTime();
					startTime = startTimeFiled.getStartTime();
					stopTime = startTimeFiled.getStopTime();

					start = new Date(startTime*1000);
					end = new Date(stopTime*1000);
				}
				//  获取支持的格式
				Vector mediaDescriptions = sdp.getMediaDescriptions(true);
				// 查看是否支持PS 负载96
				//String ip = null;
				int port = -1;
				boolean mediaTransmissionTCP = false;
				Boolean tcpActive = null;
				for (Object description : mediaDescriptions) {
					MediaDescription mediaDescription = (MediaDescription) description;
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
									// 不支持tcp主动
									responseAck(evt, Response.NOT_IMPLEMENTED, "tcp active not support"); // 目录不支持点播
									return;
								} else if ("passive".equals(setup)) {
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

				logger.info("[上级点播]用户：{}， 地址：{}:{}， ssrc：{}", username, addressStr, port, ssrc);
				Device device  = null;
				// 通过 channel 和 gbStream 是否为null 值判断来源是直播流合适国标
				if (channel != null) {
					device = storager.queryVideoDeviceByPlatformIdAndChannelId(requesterId, channelId);
					if (device == null) {
						logger.warn("点播平台{}的通道{}时未找到设备信息", requesterId, channel);
						responseAck(evt, Response.SERVER_INTERNAL_ERROR);
						return;
					}
					mediaServerItem = playService.getNewMediaServerItem(device);
					if (mediaServerItem == null) {
						logger.warn("未找到可用的zlm");
						responseAck(evt, Response.BUSY_HERE);
						return;
					}
					SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
							device.getDeviceId(), channelId,
							mediaTransmissionTCP);
					if (tcpActive != null) {
						sendRtpItem.setTcpActive(tcpActive);
					}
					if (sendRtpItem == null) {
						logger.warn("服务器端口资源不足");
						responseAck(evt, Response.BUSY_HERE);
						return;
					}
					sendRtpItem.setCallId(callIdHeader.getCallId());
					sendRtpItem.setPlayType("Play".equals(sessionName)?InviteStreamType.PLAY:InviteStreamType.PLAYBACK);
					byte[] dialogByteArray = SerializeUtils.serialize(evt.getDialog());
					sendRtpItem.setDialog(dialogByteArray);
					byte[] transactionByteArray = SerializeUtils.serialize(evt.getServerTransaction());
					sendRtpItem.setTransaction(transactionByteArray);
					Long finalStartTime = startTime;
					Long finalStopTime = stopTime;
					ZLMHttpHookSubscribe.Event hookEvent = (mediaServerItemInUSe, responseJSON)->{
						String app = responseJSON.getString("app");
						String stream = responseJSON.getString("stream");
						logger.info("[上级点播]下级已经开始推流。 回复200OK(SDP)， {}/{}", app, stream);
						//     * 0 等待设备推流上来
						//     * 1 下级已经推流，等待上级平台回复ack
						//     * 2 推流中
						sendRtpItem.setStatus(1);
						redisCatchStorage.updateSendRTPSever(sendRtpItem);

						StringBuffer content = new StringBuffer(200);
						content.append("v=0\r\n");
						content.append("o="+ channelId +" 0 0 IN IP4 "+mediaServerItemInUSe.getSdpIp()+"\r\n");
						content.append("s=" + sessionName+"\r\n");
						content.append("c=IN IP4 "+mediaServerItemInUSe.getSdpIp()+"\r\n");
						if ("Playback".equals(sessionName)) {
							content.append("t=" + finalStartTime + " " + finalStopTime + "\r\n");
						}else {
							content.append("t=0 0\r\n");
						}
						content.append("m=video "+ sendRtpItem.getLocalPort()+" RTP/AVP 96\r\n");
						content.append("a=sendonly\r\n");
						content.append("a=rtpmap:96 PS/90000\r\n");
						content.append("y="+ ssrc + "\r\n");
						content.append("f=\r\n");

						try {
							// 超时未收到Ack应该回复bye,当前等待时间为10秒
							dynamicTask.startDelay(callIdHeader.getCallId(), ()->{
								logger.info("Ack 等待超时");
								mediaServerService.releaseSsrc(mediaServerItemInUSe.getId(), ssrc);
								// 回复bye
								cmderFroPlatform.streamByeCmd(platform, callIdHeader.getCallId());
							}, 60*1000);
							responseSdpAck(evt, content.toString(), platform);

						} catch (SipException e) {
							e.printStackTrace();
						} catch (InvalidArgumentException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							e.printStackTrace();
						}
					};
					SipSubscribe.Event errorEvent = ((event) -> {
						// 未知错误。直接转发设备点播的错误
						Response response = null;
						try {
							response = getMessageFactory().createResponse(event.statusCode, evt.getRequest());
							ServerTransaction serverTransaction = getServerTransaction(evt);
							serverTransaction.sendResponse(response);
							if (serverTransaction.getDialog() != null) {
								serverTransaction.getDialog().delete();
							}
						} catch (ParseException | SipException | InvalidArgumentException e) {
							e.printStackTrace();
						}
					});
					sendRtpItem.setApp("rtp");
					if ("Playback".equals(sessionName)) {
						sendRtpItem.setPlayType(InviteStreamType.PLAYBACK);
						SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, null, true);
						sendRtpItem.setStreamId(ssrcInfo.getStream());
						// 写入redis， 超时时回复
						redisCatchStorage.updateSendRTPSever(sendRtpItem);
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						playService.playBack(mediaServerItem, ssrcInfo, device.getDeviceId(), channelId, format.format(start),
								format.format(end), null, result -> {
								if (result.getCode() != 0){
									logger.warn("录像回放失败");
									if (result.getEvent() != null) {
										errorEvent.response(result.getEvent());
									}
									redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
									try {
										responseAck(evt, Response.REQUEST_TIMEOUT);
									} catch (SipException e) {
										e.printStackTrace();
									} catch (InvalidArgumentException e) {
										e.printStackTrace();
									} catch (ParseException e) {
										e.printStackTrace();
									}
								}else {
									if (result.getMediaServerItem() != null) {
										hookEvent.response(result.getMediaServerItem(), result.getResponse());
									}
								}
							});
					}else {
						sendRtpItem.setPlayType(InviteStreamType.PLAY);
						SsrcTransaction playTransaction = sessionManager.getSsrcTransaction(device.getDeviceId(), channelId, "play", null);
						if (playTransaction != null) {
							Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, "rtp", playTransaction.getStream());
							if (!streamReady) {
								playTransaction = null;
							}
						}
						if (playTransaction == null) {
							String streamId = null;
							if (mediaServerItem.isRtpEnable()) {
								streamId = String.format("%s_%s", device.getDeviceId(), channelId);
							}
							SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, true);
							sendRtpItem.setStreamId(ssrcInfo.getStream());
							// 写入redis， 超时时回复
							redisCatchStorage.updateSendRTPSever(sendRtpItem);
							playService.play(mediaServerItem, ssrcInfo, device, channelId, hookEvent, errorEvent, (code, msg)->{
								redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
							}, null);
						}else {
							sendRtpItem.setStreamId(playTransaction.getStream());
							// 写入redis， 超时时回复
							redisCatchStorage.updateSendRTPSever(sendRtpItem);
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("app", sendRtpItem.getApp());
							jsonObject.put("stream", sendRtpItem.getStreamId());
							hookEvent.response(mediaServerItem, jsonObject);
						}
					}
				}else if (gbStream != null) {

					Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, gbStream.getApp(), gbStream.getStream());
					if (!streamReady ) {
						if ("proxy".equals(gbStream.getStreamType())) {
							// TODO 控制启用以使设备上线
							logger.info("[ app={}, stream={} ]通道离线，启用流后开始推流",gbStream.getApp(), gbStream.getStream());
							responseAck(evt, Response.BAD_REQUEST, "channel [" + gbStream.getGbId() + "] offline");
						}else if ("push".equals(gbStream.getStreamType())) {
							if (!platform.isStartOfflinePush()) {
								responseAck(evt, Response.TEMPORARILY_UNAVAILABLE, "channel unavailable");
								return;
							}
							// 发送redis消息以使设备上线
							logger.info("[ app={}, stream={} ]通道离线，发送redis信息控制设备开始推流",gbStream.getApp(), gbStream.getStream());
							MessageForPushChannel messageForPushChannel = new MessageForPushChannel();
							messageForPushChannel.setType(1);
							messageForPushChannel.setGbId(gbStream.getGbId());
							messageForPushChannel.setApp(gbStream.getApp());
							messageForPushChannel.setStream(gbStream.getStream());
							// TODO 获取低负载的节点
							messageForPushChannel.setMediaServerId(gbStream.getMediaServerId());
							messageForPushChannel.setPlatFormId(platform.getServerGBId());
							messageForPushChannel.setPlatFormName(platform.getName());
							redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);
							// 设置超时
							dynamicTask.startDelay(callIdHeader.getCallId(), ()->{
								logger.info("[ app={}, stream={} ] 等待设备开始推流超时", gbStream.getApp(), gbStream.getStream());
								try {
									mediaListManager.removedChannelOnlineEventLister(gbStream.getGbId());
									responseAck(evt, Response.REQUEST_TIMEOUT); // 超时
								} catch (SipException e) {
									e.printStackTrace();
								} catch (InvalidArgumentException e) {
									e.printStackTrace();
								} catch (ParseException e) {
									e.printStackTrace();
								}
							}, userSetting.getPlatformPlayTimeout());
							// 添加监听
							MediaServerItem finalMediaServerItem = mediaServerItem;
							int finalPort = port;
							boolean finalMediaTransmissionTCP = mediaTransmissionTCP;
							Boolean finalTcpActive = tcpActive;
							mediaListManager.addChannelOnlineEventLister(gbStream.getGbId(), (app, stream)->{
								SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(finalMediaServerItem, addressStr, finalPort, ssrc, requesterId,
										app, stream, channelId, finalMediaTransmissionTCP);

								if (sendRtpItem == null) {
									logger.warn("服务器端口资源不足");
									try {
										responseAck(evt, Response.BUSY_HERE);
									} catch (SipException e) {
										e.printStackTrace();
									} catch (InvalidArgumentException e) {
										e.printStackTrace();
									} catch (ParseException e) {
										e.printStackTrace();
									}
									return;
								}
								if (finalTcpActive != null) {
									sendRtpItem.setTcpActive(finalTcpActive);
								}
								sendRtpItem.setPlayType(InviteStreamType.PUSH);
								// 写入redis， 超时时回复
								sendRtpItem.setStatus(1);
								sendRtpItem.setCallId(callIdHeader.getCallId());
								byte[] dialogByteArray = SerializeUtils.serialize(evt.getDialog());
								sendRtpItem.setDialog(dialogByteArray);
								byte[] transactionByteArray = SerializeUtils.serialize(evt.getServerTransaction());
								sendRtpItem.setTransaction(transactionByteArray);
								redisCatchStorage.updateSendRTPSever(sendRtpItem);
								sendStreamAck(finalMediaServerItem, sendRtpItem, platform, evt);

							});
						}
					}else {
						SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
								gbStream.getApp(), gbStream.getStream(), channelId,
								mediaTransmissionTCP);


						if (sendRtpItem == null) {
							logger.warn("服务器端口资源不足");
							responseAck(evt, Response.BUSY_HERE);
							return;
						}
						if (tcpActive != null) {
							sendRtpItem.setTcpActive(tcpActive);
						}
						sendRtpItem.setPlayType(InviteStreamType.PUSH);
						// 写入redis， 超时时回复
						sendRtpItem.setStatus(1);
						sendRtpItem.setCallId(callIdHeader.getCallId());
						byte[] dialogByteArray = SerializeUtils.serialize(evt.getDialog());
						sendRtpItem.setDialog(dialogByteArray);
						byte[] transactionByteArray = SerializeUtils.serialize(evt.getServerTransaction());
						sendRtpItem.setTransaction(transactionByteArray);
						redisCatchStorage.updateSendRTPSever(sendRtpItem);
						sendStreamAck(mediaServerItem, sendRtpItem, platform, evt);
					}


				}

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

	public void sendStreamAck(MediaServerItem mediaServerItem, SendRtpItem sendRtpItem, ParentPlatform platform, RequestEvent evt){

		StringBuffer content = new StringBuffer(200);
		content.append("v=0\r\n");
		content.append("o="+ sendRtpItem.getChannelId() +" 0 0 IN IP4 "+ mediaServerItem.getSdpIp()+"\r\n");
		content.append("s=Play\r\n");
		content.append("c=IN IP4 "+mediaServerItem.getSdpIp()+"\r\n");
		content.append("t=0 0\r\n");
		content.append("m=video "+ sendRtpItem.getLocalPort()+" RTP/AVP 96\r\n");
		content.append("a=sendonly\r\n");
		content.append("a=rtpmap:96 PS/90000\r\n");
		if (sendRtpItem.isTcp()) {
			content.append("a=connection:new\r\n");
			if (!sendRtpItem.isTcpActive()) {
				content.append("a=setup:active\r\n");
			}else {
				content.append("a=setup:passive\r\n");
			}
		}
		content.append("y="+ sendRtpItem.getSsrc() + "\r\n");
		content.append("f=\r\n");

		try {
			responseSdpAck(evt, content.toString(), platform);
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void inviteFromDeviceHandle(RequestEvent evt, String requesterId) throws InvalidArgumentException, ParseException, SipException, SdpException {

		// 非上级平台请求，查询是否设备请求（通常为接收语音广播的设备）
		Device device = redisCatchStorage.getDevice(requesterId);
		Request request = evt.getRequest();
		if (device != null) {
			logger.info("收到设备" + requesterId + "的语音广播Invite请求");
			responseAck(evt, Response.TRYING);

			String contentString = new String(request.getRawContent());
			// jainSip不支持y=字段， 移除移除以解析。
			String substring = contentString;
			String ssrc = "0000000404";
			int ssrcIndex = contentString.indexOf("y=");
			if (ssrcIndex > 0) {
				substring = contentString.substring(0, ssrcIndex);
				ssrc = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
			}
			ssrcIndex = substring.indexOf("f=");
			if (ssrcIndex > 0) {
				substring = contentString.substring(0, ssrcIndex);
			}
			SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);

			//  获取支持的格式
			Vector mediaDescriptions = sdp.getMediaDescriptions(true);
			// 查看是否支持PS 负载96
			int port = -1;
			//boolean recvonly = false;
			boolean mediaTransmissionTCP = false;
			Boolean tcpActive = null;
			for (int i = 0; i < mediaDescriptions.size(); i++) {
				MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(i);
				Media media = mediaDescription.getMedia();

				Vector mediaFormats = media.getMediaFormats(false);
				if (mediaFormats.contains("8")) {
					port = media.getMediaPort();
					String protocol = media.getProtocol();
					// 区分TCP发流还是udp， 当前默认udp
					if ("TCP/RTP/AVP".equals(protocol)) {
						String setup = mediaDescription.getAttribute("setup");
						if (setup != null) {
							mediaTransmissionTCP = true;
							if ("active".equals(setup)) {
								tcpActive = true;
							} else if ("passive".equals(setup)) {
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
			logger.info("设备{}请求语音流，地址：{}:{}，ssrc：{}", username, addressStr, port, ssrc);

		} else {
			logger.warn("来自无效设备/平台的请求");
			responseAck(evt, Response.BAD_REQUEST);
		}
	}
}
