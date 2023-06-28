package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * SIP命令类型： BYE请求
 */
@Component
public class ByeRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final Logger logger = LoggerFactory.getLogger(ByeRequestProcessor.class);
	private final String method = "BYE";

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	private UserSetting userSetting;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * 处理BYE请求
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {

		try {
			responseAck((SIPRequest) evt.getRequest(), Response.OK);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[回复BYE信息失败]，{}", e.getMessage());
		}
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
			String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId, null, callIdHeader.getCallId());
			logger.info("[收到bye] {}/{}", platformGbId, channelId);
			if (sendRtpItem != null){
				String streamId = sendRtpItem.getStreamId();
				Map<String, Object> param = new HashMap<>();
				param.put("vhost","__defaultVhost__");
				param.put("app",sendRtpItem.getApp());
				param.put("stream",streamId);
				param.put("ssrc",sendRtpItem.getSsrc());
				logger.info("[收到bye] 停止向上级推流：{}", streamId);
				MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
				redisCatchStorage.deleteSendRTPServer(platformGbId, channelId, callIdHeader.getCallId(), null);
				zlmrtpServerFactory.stopSendRtpStream(mediaInfo, param);
				if (sendRtpItem.getPlayType().equals(InviteStreamType.PUSH)) {
					ParentPlatform platform = platformService.queryPlatformByServerGBId(sendRtpItem.getPlatformId());
					if (platform != null) {
						MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
								sendRtpItem.getApp(), sendRtpItem.getStreamId(), sendRtpItem.getChannelId(),
								sendRtpItem.getPlatformId(), platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
						messageForPushChannel.setPlatFormIndex(platform.getId());
						redisCatchStorage.sendPlatformStopPlayMsg(messageForPushChannel);
					}else {
						logger.info("[上级平台停止观看] 未找到平台{}的信息，发送redis消息失败", sendRtpItem.getPlatformId());
					}
				}

				int totalReaderCount = zlmrtpServerFactory.totalReaderCount(mediaInfo, sendRtpItem.getApp(), streamId);
				if (totalReaderCount <= 0) {
					logger.info("[收到bye] {} 无其它观看者，通知设备停止推流", streamId);
					if (sendRtpItem.getPlayType().equals(InviteStreamType.PLAY)) {
						Device device = deviceService.getDevice(sendRtpItem.getDeviceId());
						if (device == null) {
							logger.info("[收到bye] {} 通知设备停止推流时未找到设备信息", streamId);
						}
						try {
							logger.warn("[停止点播] {}/{}", sendRtpItem.getDeviceId(), channelId);
							cmder.streamByeCmd(device, channelId, streamId, null);
						} catch (InvalidArgumentException | ParseException | SipException |
								 SsrcTransactionNotFoundException e) {
							logger.error("[收到bye] {} 无其它观看者，通知设备停止推流， 发送BYE失败 {}",streamId, e.getMessage());
						}
					}
//					if (sendRtpItem.getPlayType().equals(InviteStreamType.PUSH)) {
//						MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
//								sendRtpItem.getApp(), sendRtpItem.getStreamId(), sendRtpItem.getChannelId(),
//								sendRtpItem.getPlatformId(), null, null, sendRtpItem.getMediaServerId());
//						redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);
//					}
				}
			}
			// 可能是设备主动停止
			Device device = storager.queryVideoDeviceByChannelId(platformGbId);
			if (device != null) {
				storager.stopPlay(device.getDeviceId(), channelId);
				StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(device.getDeviceId(), channelId);
				if (streamInfo != null) {
					redisCatchStorage.stopPlay(streamInfo);
					mediaServerService.closeRTPServer(streamInfo.getMediaServerId(), streamInfo.getStream());
				}
				SsrcTransaction ssrcTransactionForPlay = streamSession.getSsrcTransaction(device.getDeviceId(), channelId, "play", null);
				if (ssrcTransactionForPlay != null){
					if (ssrcTransactionForPlay.getCallId().equals(callIdHeader.getCallId())){
						// 释放ssrc
						MediaServerItem mediaServerItem = mediaServerService.getOne(ssrcTransactionForPlay.getMediaServerId());
						if (mediaServerItem != null) {
							mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcTransactionForPlay.getSsrc());
						}
						streamSession.remove(device.getDeviceId(), channelId, ssrcTransactionForPlay.getStream());
					}
				}
				SsrcTransaction ssrcTransactionForPlayBack = streamSession.getSsrcTransaction(device.getDeviceId(), channelId, callIdHeader.getCallId(), null);
				if (ssrcTransactionForPlayBack != null) {
					// 释放ssrc
					MediaServerItem mediaServerItem = mediaServerService.getOne(ssrcTransactionForPlayBack.getMediaServerId());
					if (mediaServerItem != null) {
						mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcTransactionForPlayBack.getSsrc());
					}
					streamSession.remove(device.getDeviceId(), channelId, ssrcTransactionForPlayBack.getStream());
				}
			}

	}
}
