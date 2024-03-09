package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.IStreamSendManager;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.*;
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
import javax.sip.header.CallIdHeader;
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
	private IStreamSendManager streamSendManager;

	@Autowired
	private ICommonGbChannelService commonGbChannelService;

	@Autowired
	private ISIPCommanderForPlatform commanderForPlatform;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private AudioBroadcastManager audioBroadcastManager;

	@Autowired
	private IDeviceChannelService channelService;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private ZLMServerFactory zlmServerFactory;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	private IPlayService playService;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private Map<String, IResourceService> resourceServiceMap;

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
		SIPRequest request = (SIPRequest) evt.getRequest();
		try {
			responseAck(request, Response.OK);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[回复BYE信息失败]，{}", e.getMessage());
		}
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);

		SendRtpItem sendRtpItem = streamSendManager.getByCallId(callIdHeader.getCallId());
		if (sendRtpItem != null){
			logger.info("[收到bye] 来自{}， 停止通道：{}", sendRtpItem.getDestId(), sendRtpItem.getChannelId());
			String streamId = sendRtpItem.getStreamId();
			Map<String, Object> param = new HashMap<>();
			param.put("vhost","__defaultVhost__");
			param.put("app",sendRtpItem.getApp());
			param.put("stream",streamId);
			param.put("ssrc",sendRtpItem.getSsrc());
			logger.info("[收到bye] 停止推流：{}", streamId);
			MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
			zlmServerFactory.stopSendRtpStream(mediaInfo, param);
			streamSendManager.remove(sendRtpItem);
			CommonGbChannel channel = commonGbChannelService.getChannel(sendRtpItem.getChannelId());
			IResourceService resourceService = resourceServiceMap.get(channel.getType());
			if (resourceService != null) {
				resourceService.stopPlay(channel, null);
			}
			if (userSetting.getUseCustomSsrcForParentInvite()) {
				mediaServerService.releaseSsrc(mediaInfo.getId(), sendRtpItem.getSsrc());
			}
			if (sendRtpItem.getPlayType().equals(InviteStreamType.PUSH)) {
				ParentPlatform platform = platformService.queryPlatformByServerGBId(sendRtpItem.getDestId());
				if (platform != null) {
					MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
							sendRtpItem.getApp(), sendRtpItem.getStreamId(), sendRtpItem.getChannelId(),
							sendRtpItem.getDestId(), platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
					messageForPushChannel.setPlatFormIndex(platform.getId());
					redisCatchStorage.sendPlatformStopPlayMsg(messageForPushChannel);
				}else {
					logger.info("[上级平台停止观看] 未找到平台{}的信息，发送redis消息失败", sendRtpItem.getPlatformId());
				}
			}

			AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
			if (audioBroadcastCatch != null && audioBroadcastCatch.getSipTransactionInfo().getCallId().equals(callIdHeader.getCallId())) {
				// 来自上级平台的停止对讲
				logger.info("[停止对讲] 来自上级，平台：{}, 通道：{}", sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
				audioBroadcastManager.del(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
			}

			int totalReaderCount = zlmServerFactory.totalReaderCount(mediaInfo, sendRtpItem.getApp(), streamId);
			if (totalReaderCount <= 0) {
				logger.info("[收到bye] {} 无其它观看者，通知设备停止推流", streamId);
				if (sendRtpItem.getPlayType().equals(InviteStreamType.PLAY)) {
					Device device = deviceService.getDevice(sendRtpItem.getDeviceId());
					if (device == null) {
						logger.info("[收到bye] {} 通知设备停止推流时未找到设备信息", streamId);
					}
					try {
						logger.info("[停止点播] {}/{}", sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
						cmder.streamByeCmd(device, sendRtpItem.getChannelId(), streamId, null);
					} catch (InvalidArgumentException | ParseException | SipException |
							 SsrcTransactionNotFoundException e) {
						logger.error("[收到bye] {} 无其它观看者，通知设备停止推流， 发送BYE失败 {}",streamId, e.getMessage());
					}
				}
			}
		}

			// 可能是设备发送的停止
			SsrcTransaction ssrcTransaction = streamSession.getSsrcTransactionByCallId(callIdHeader.getCallId());
			if (ssrcTransaction == null) {
				return;
			}
			logger.info("[收到bye] 来自设备：{}, 通道已停止推流: {}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());

		ParentPlatform platform = platformService.queryPlatformByServerGBId(ssrcTransaction.getDeviceId());
		if (platform != null ) {
			if (ssrcTransaction.getType().equals(InviteSessionType.BROADCAST)) {
				logger.info("[收到bye] 上级停止语音对讲，来自：{}, 通道已停止推流: {}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
				DeviceChannel channel = storager.queryChannelInParentPlatform(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
				if (channel == null) {
					logger.info("[收到bye] 未找到通道，设备：{}， 通道：{}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
					return;
				}
				String mediaServerId = ssrcTransaction.getMediaServerId();
				platformService.stopBroadcast(platform, channel, ssrcTransaction.getStream(), false,
						mediaServerService.getOne(mediaServerId));

				playService.stopAudioBroadcast(channel.getDeviceId(), channel.getChannelId());
			}

		}else {
			Device device = deviceService.getDevice(ssrcTransaction.getDeviceId());
			if (device == null) {
				logger.info("[收到bye] 未找到设备：{} ", ssrcTransaction.getDeviceId());
				return;
			}
			DeviceChannel channel = channelService.getOne(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
			if (channel == null) {
				logger.info("[收到bye] 未找到通道，设备：{}， 通道：{}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
				return;
			}
			storager.stopPlay(device.getDeviceId(), channel.getChannelId());
			InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId());
			if (inviteInfo != null) {
				inviteStreamService.removeInviteInfo(inviteInfo);
				if (inviteInfo.getStreamInfo() != null) {
					mediaServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServerId(), inviteInfo.getStreamInfo().getStream());
				}
			}
			// 释放ssrc
			MediaServerItem mediaServerItem = mediaServerService.getOne(ssrcTransaction.getMediaServerId());
			if (mediaServerItem != null) {
				mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcTransaction.getSsrc());
			}
			streamSession.removeByCallId(device.getDeviceId(), channel.getChannelId(), ssrcTransaction.getCallId());
			if (ssrcTransaction.getType() == InviteSessionType.BROADCAST) {
				// 查找来源的对讲设备，发送停止
				Device sourceDevice = storager.queryVideoDeviceByPlatformIdAndChannelId(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
				if (sourceDevice != null) {
					playService.stopAudioBroadcast(sourceDevice.getDeviceId(), channel.getChannelId());
				}
			}
			AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(ssrcTransaction.getDeviceId(), channel.getChannelId());
			if (audioBroadcastCatch != null) {
				// 来自上级平台的停止对讲
				logger.info("[停止对讲] 来自上级，平台：{}, 通道：{}", ssrcTransaction.getDeviceId(), channel.getChannelId());
				audioBroadcastManager.del(ssrcTransaction.getDeviceId(), channel.getChannelId());
			}
		}
	}
}
