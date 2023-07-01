package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
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
	private ZLMRTPServerFactory zlmrtpServerFactory;

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

		String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		logger.info("[收到ACK]： platformGbId->{}", platformGbId);
		if (userSetting.getPushStreamAfterAck()) {
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformGbId);
			// 取消设置的超时任务
			dynamicTask.stop(callIdHeader.getCallId());
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(null, null, null, callIdHeader.getCallId());
			if (sendRtpItem == null) {
				logger.warn("[收到ACK]：未找到通道({})的推流信息", channelId);
				return;
			}
			String isUdp = sendRtpItem.isTcp() ? "0" : "1";
			MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
			logger.info("收到ACK，rtp/{}开始级推流, 目标={}:{}，SSRC={}, RTCP={}", sendRtpItem.getStream(),
					sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
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

			if (mediaInfo == null) {
				RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
						sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStream(),
						sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
						sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
				redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, json -> {
					playService.startSendRtpStreamHand(sendRtpItem, parentPlatform, json, param, callIdHeader);
				});
			} else {
				// 如果是非严格模式，需要关闭端口占用
				JSONObject startSendRtpStreamResult = null;
				if (sendRtpItem.getLocalPort() != 0) {
					if (sendRtpItem.isTcpActive()) {
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
					}else {
						param.put("dst_url", sendRtpItem.getIp());
						param.put("dst_port", sendRtpItem.getPort());
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
					}
				}else {
					if (sendRtpItem.isTcpActive()) {
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
					}else {
						param.put("dst_url", sendRtpItem.getIp());
						param.put("dst_port", sendRtpItem.getPort());
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
					}
				}
				if (startSendRtpStreamResult != null) {
					playService.startSendRtpStreamHand(sendRtpItem, parentPlatform, startSendRtpStreamResult, param, callIdHeader);
				}
			}
		}
	}

}
