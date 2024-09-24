package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;

/**
 * SIP命令类型： ACK请求
 * @author lin
 */
@Slf4j
@Component
public class AckRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "ACK";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	@Autowired
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
    private IRedisRpcService redisRpcService;

	@Autowired
    private UserSetting userSetting;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private IPlayService playService;

	@Autowired
	private ISendRtpServerService sendRtpServerService;


	/**   
	 * 处理  ACK请求
	 */
	@Override
	public void process(RequestEvent evt) {
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
		dynamicTask.stop(callIdHeader.getCallId());
		String fromUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		String toUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
		log.info("[收到ACK]： 来自->{}", fromUserId);
		SendRtpInfo sendRtpItem =  sendRtpServerService.queryByCallId(callIdHeader.getCallId());
		if (sendRtpItem == null) {
			log.warn("[收到ACK]：未找到来自{}，callId: {}", fromUserId, callIdHeader.getCallId());
			return;
		}
		// tcp主动时，此时是级联下级平台，在回复200ok时，本地已经请求zlm开启监听，跳过下面步骤
		if (sendRtpItem.isTcpActive()) {
			log.info("收到ACK，rtp/{} TCP主动方式后续处理", sendRtpItem.getStream());
			return;
		}
		MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
		log.info("收到ACK，rtp/{}开始向上级推流, 目标={}:{}，SSRC={}, 协议:{}",
				sendRtpItem.getStream(),
				sendRtpItem.getIp(),
				sendRtpItem.getPort(),
				sendRtpItem.getSsrc(),
				sendRtpItem.isTcp()?(sendRtpItem.isTcpActive()?"TCP主动":"TCP被动"):"UDP"
		);
		Platform parentPlatform = platformService.queryPlatformByServerGBId(fromUserId);

		if (parentPlatform != null) {
			DeviceChannel deviceChannel = deviceChannelService.getOneById(sendRtpItem.getChannelId());
			if (!userSetting.getServerId().equals(sendRtpItem.getServerId())) {
				WVPResult wvpResult = redisRpcService.startSendRtp(sendRtpItem.getChannelId(), sendRtpItem);
				if (wvpResult.getCode() == 0) {
					redisCatchStorage.sendPlatformStartPlayMsg(sendRtpItem, deviceChannel, parentPlatform);
				}
			} else {
				try {
					if (sendRtpItem.isTcpActive()) {
						mediaServerService.startSendRtpPassive(mediaInfo,sendRtpItem, null);
					} else {
						mediaServerService.startSendRtp(mediaInfo, sendRtpItem);
					}
					redisCatchStorage.sendPlatformStartPlayMsg(sendRtpItem, deviceChannel, parentPlatform);
				}catch (ControllerException e) {
					log.error("RTP推流失败: {}", e.getMessage());
					playService.startSendRtpStreamFailHand(sendRtpItem, parentPlatform, callIdHeader);
				}
			}
		}else {
			Device device = deviceService.getDeviceByDeviceId(fromUserId);
			if (device == null) {
				log.warn("[收到ACK]：来自{}，目标为({})的推流信息为找到流体服务[{}]信息",fromUserId, toUserId, sendRtpItem.getMediaServerId());
				return;
			}
			// 设置为收到ACK后发送语音的设备已经在发送200OK开始发流了
			if (!device.isBroadcastPushAfterAck()) {
				return;
			}
			if (mediaInfo == null) {
				log.warn("[收到ACK]：来自{}，目标为({})的推流信息为找到流体服务[{}]信息",fromUserId, toUserId, sendRtpItem.getMediaServerId());
				return;
			}
			try {
				if (sendRtpItem.isTcpActive()) {
					mediaServerService.startSendRtpPassive(mediaInfo, sendRtpItem, null);
				} else {
					mediaServerService.startSendRtp(mediaInfo, sendRtpItem);
				}
			}catch (ControllerException e) {
				log.error("RTP推流失败: {}", e.getMessage());
				playService.startSendRtpStreamFailHand(sendRtpItem, null, callIdHeader);
			}
		}
	}

}
