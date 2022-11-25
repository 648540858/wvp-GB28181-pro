package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatch;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * SIP命令类型： ACK请求
 * @author lin
 */
@Component
public class AckRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private Logger logger = LoggerFactory.getLogger(AckRequestProcessor.class);
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
	private IVideoManagerStorage storager;

	@Autowired
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private ZlmHttpHookSubscribe hookSubscribe;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private ZlmHttpHookSubscribe subscribe;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private ISIPCommanderForPlatform commanderForPlatform;

	@Autowired
	private AudioBroadcastManager audioBroadcastManager;

	@Autowired
	private RedisGbPlayMsgListener redisGbPlayMsgListener;


	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);

		String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		logger.info("[收到ACK]： platformGbId->{}", platformGbId);
		ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformGbId);
		// 取消设置的超时任务
		dynamicTask.stop(callIdHeader.getCallId());
		String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
		SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(null, null, null, callIdHeader.getCallId());
		if (sendRtpItem == null) {
			logger.warn("[收到ACK]：未找到通道({})的推流信息", channelId);
			return;
		}
		String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
		MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
		logger.info("收到ACK，rtp/{}开始向上级推流, 目标={}:{}，SSRC={}, RTCP={}", sendRtpItem.getStreamId(),
				sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
		Map<String, Object> param = new HashMap<>(12);
		param.put("vhost","__defaultVhost__");
		param.put("app",sendRtpItem.getApp());
		param.put("stream",sendRtpItem.getStreamId());
		param.put("ssrc", sendRtpItem.getSsrc());
		param.put("src_port", sendRtpItem.getLocalPort());
		param.put("pt", sendRtpItem.getPt());
		param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
		param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
		if (!sendRtpItem.isTcp()) {
			// udp模式下开启rtcp保活
			param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
		}

		if (mediaInfo == null) {
			RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
					sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStreamId(),
					sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
					sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
			redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, json -> {
				startSendRtpStreamHand(evt, sendRtpItem, parentPlatform, json, param, callIdHeader);
			});
		} else {
			// 如果是非严格模式，需要关闭端口占用
			JSONObject startSendRtpStreamResult = null;
			if (sendRtpItem.getLocalPort() != 0) {
				HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(sendRtpItem.getSsrc(), null, mediaInfo.getId());
				hookSubscribe.removeSubscribe(hookSubscribeForRtpServerTimeout);
				if (zlmrtpServerFactory.releasePort(mediaInfo, sendRtpItem.getSsrc())) {
					if (sendRtpItem.isTcpActive()) {
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
					}else {
						param.put("is_udp", is_Udp);
						param.put("dst_url", sendRtpItem.getIp());
						param.put("dst_port", sendRtpItem.getPort());
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
					}
				}
			}else {
				if (sendRtpItem.isTcpActive()) {
					startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
				}else {
					param.put("is_udp", is_Udp);
					param.put("dst_url", sendRtpItem.getIp());
					param.put("dst_port", sendRtpItem.getPort());
					startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
				}
			}
			if (startSendRtpStreamResult != null) {
				startSendRtpStreamHand(evt, sendRtpItem, parentPlatform, startSendRtpStreamResult, param, callIdHeader);
			}
		}
	}
	private void startSendRtpStreamHand(RequestEvent evt, SendRtpItem sendRtpItem, ParentPlatform parentPlatform,
										JSONObject jsonObject, Map<String, Object> param, CallIdHeader callIdHeader) {
		if (jsonObject == null) {
			logger.error("RTP推流失败: 请检查ZLM服务");
		} else if (jsonObject.getInteger("code") == 0) {
			logger.info("调用ZLM推流接口, 结果： {}",  jsonObject);
			logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, " ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
		} else {
			logger.error("RTP推流失败: {}, 参数：{}",jsonObject.getString("msg"), JSON.toJSONString(param));
			if (sendRtpItem.isOnlyAudio()) {
				Device device = deviceService.getDevice(sendRtpItem.getDeviceId());
				AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
				if (audioBroadcastCatch != null) {
					try {
						cmder.streamByeCmd(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
					} catch (SipException | ParseException | InvalidArgumentException |
							 SsrcTransactionNotFoundException e) {
						logger.error("[命令发送失败] 停止语音对讲: {}", e.getMessage());
					}
				}
			}else {
				// 向上级平台
				try {
					commanderForPlatform.streamByeCmd(parentPlatform, callIdHeader.getCallId());
				} catch (SipException | InvalidArgumentException | ParseException e) {
					logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
				}
			}
		}
	}
}
