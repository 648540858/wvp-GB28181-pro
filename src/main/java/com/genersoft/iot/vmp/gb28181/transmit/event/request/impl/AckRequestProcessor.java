package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import java.util.HashMap;
import java.util.Map;

/**
 * SIP命令类型： ACK请求
 * @author lin
 */
@Component
public class AckRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final Logger logger = LoggerFactory.getLogger(AckRequestProcessor.class);
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
    private UserSetting userSetting;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private ZLMServerFactory zlmServerFactory;

	@Autowired
	private ZlmHttpHookSubscribe hookSubscribe;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private RedisGbPlayMsgListener redisGbPlayMsgListener;

	@Autowired
	private IPlayService playService;


	/**   
	 * 处理  ACK请求
	 */
	@Override
	public void process(RequestEvent evt) {
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
		dynamicTask.stop(callIdHeader.getCallId());
		String fromUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		String toUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
		logger.info("[收到ACK]： 来自->{}", fromUserId);
		SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(null, null, null, callIdHeader.getCallId());
		if (sendRtpItem == null) {
			logger.warn("[收到ACK]：未找到来自{}，目标为({})的推流信息",fromUserId, toUserId);
			return;
		}
		// tcp主动时，此时是级联下级平台，在回复200ok时，本地已经请求zlm开启监听，跳过下面步骤
		if (sendRtpItem.isTcpActive()) {
			logger.info("收到ACK，rtp/{} TCP主动方式后续处理", sendRtpItem.getStream());
			return;
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
		logger.info("收到ACK，rtp/{}开始向上级推流, 目标={}:{}，SSRC={}, 协议:{}",
				sendRtpItem.getStream(),
				sendRtpItem.getIp(),
				sendRtpItem.getPort(),
				sendRtpItem.getSsrc(),
				sendRtpItem.isTcp()?(sendRtpItem.isTcpActive()?"TCP主动":"TCP被动"):"UDP"
		);
		ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(fromUserId);

		if (parentPlatform != null) {
			Map<String, Object> param = getSendRtpParam(sendRtpItem);
			if (mediaInfo == null) {
				RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
						sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStream(),
						sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
						sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
				redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, json -> {
					playService.startSendRtpStreamHand(sendRtpItem, parentPlatform, json, param, callIdHeader);
				});
			} else {
				JSONObject startSendRtpStreamResult = sendRtp(sendRtpItem, mediaInfo, param);
				if (startSendRtpStreamResult != null) {
					playService.startSendRtpStreamHand(sendRtpItem, parentPlatform, startSendRtpStreamResult, param, callIdHeader);
				}
			}
		}else {
			Device device = deviceService.getDevice(fromUserId);
			if (device == null) {
				logger.warn("[收到ACK]：来自{}，目标为({})的推流信息为找到流体服务[{}]信息",fromUserId, toUserId, sendRtpItem.getMediaServerId());
				return;
			}
			// 设置为收到ACK后发送语音的设备已经在发送200OK开始发流了
			if (!device.isBroadcastPushAfterAck()) {
				return;
			}
			if (mediaInfo == null) {
				logger.warn("[收到ACK]：来自{}，目标为({})的推流信息为找到流体服务[{}]信息",fromUserId, toUserId, sendRtpItem.getMediaServerId());
				return;
			}
			Map<String, Object> param = getSendRtpParam(sendRtpItem);
			JSONObject startSendRtpStreamResult = sendRtp(sendRtpItem, mediaInfo, param);
			if (startSendRtpStreamResult != null) {
				playService.startSendRtpStreamHand(sendRtpItem, device, startSendRtpStreamResult, param, callIdHeader);
			}
		}
	}

	private Map<String, Object> getSendRtpParam(SendRtpItem sendRtpItem) {
		String isUdp = sendRtpItem.isTcp() ? "0" : "1";
		Map<String, Object> param = new HashMap<>(12);
		param.put("vhost","__defaultVhost__");
		param.put("app",sendRtpItem.getApp());
		param.put("stream",sendRtpItem.getStream());
		param.put("ssrc", sendRtpItem.getSsrc());
		param.put("dst_url",sendRtpItem.getIp());
		param.put("dst_port", sendRtpItem.getPort());
		param.put("src_port", sendRtpItem.getLocalPort());
		param.put("pt", sendRtpItem.getPt());
		param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
		param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
		param.put("is_udp", isUdp);
		if (!sendRtpItem.isTcp()) {
			// udp模式下开启rtcp保活
			param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
		}
		return param;
	}

	private JSONObject sendRtp(SendRtpItem sendRtpItem, MediaServerItem mediaInfo, Map<String, Object> param){
		JSONObject startSendRtpStreamResult = null;
		if (sendRtpItem.getLocalPort() != 0) {
			if (sendRtpItem.isTcpActive()) {
				startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
			}else {
				param.put("dst_url", sendRtpItem.getIp());
				param.put("dst_port", sendRtpItem.getPort());
				startSendRtpStreamResult = zlmServerFactory.startSendRtpStream(mediaInfo, param);
			}
		}else {
			if (sendRtpItem.isTcpActive()) {
				startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
			}else {
				param.put("dst_url", sendRtpItem.getIp());
				param.put("dst_port", sendRtpItem.getPort());
				startSendRtpStreamResult = zlmServerFactory.startSendRtpStream(mediaInfo, param);
			}
		}
		return startSendRtpStreamResult;

	}

}
