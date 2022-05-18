package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPDialog;
import org.ehcache.shadow.org.terracotta.offheapstore.storage.IntegerStorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import java.text.ParseException;
import java.util.*;

/**
 * SIP命令类型： ACK请求
 * @author lin
 */
@Component
public class AckRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private Logger logger = LoggerFactory.getLogger(AckRequestProcessor.class);
	private String method = "ACK";

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
	private IMediaServerService mediaServerService;

	@Autowired
	private ZLMHttpHookSubscribe subscribe;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private ISIPCommanderForPlatform commanderForPlatform;

	@Autowired
	private AudioBroadcastManager audioBroadcastManager;


	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		Dialog dialog = evt.getDialog();
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
		if (dialog == null) {
			return;
		}
		if (dialog.getState()== DialogState.CONFIRMED) {
			String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
			logger.info("ACK请求： platformGbId->{}", platformGbId);
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformGbId);
			// 取消设置的超时任务
			dynamicTask.stop(callIdHeader.getCallId());
//			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, null, null, callIdHeader.getCallId());
			String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
			MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
			logger.info("[收到ACK]，开始使用{}向上级推流 {}/{}->{}:{}({})", sendRtpItem.isTcp() ? "TCP" : "UDP",
					sendRtpItem.getApp(), sendRtpItem.getStreamId(),
					sendRtpItem.getIp() ,sendRtpItem.getPort(),
					sendRtpItem.getSsrc());
			Map<String, Object> param = new HashMap<>();
			param.put("vhost","__defaultVhost__");
			param.put("app",sendRtpItem.getApp());
			param.put("stream",sendRtpItem.getStreamId());
			param.put("ssrc", sendRtpItem.getSsrc());
			param.put("src_port", sendRtpItem.getLocalPort());
			param.put("pt", sendRtpItem.getPt());
			param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
			param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
			JSONObject jsonObject;
			if (sendRtpItem.isTcpActive()) {
				jsonObject = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
			}else {
				param.put("is_udp", is_Udp);
				param.put("dst_url",sendRtpItem.getIp());
				param.put("dst_port", sendRtpItem.getPort());
				jsonObject = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
			}

			if (jsonObject == null) {
				logger.error("RTP推流失败: 请检查ZLM服务");
			} else if (jsonObject.getInteger("code") == 0) {

				if (sendRtpItem.isOnlyAudio()) {
					AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
					audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.Ok);
					audioBroadcastCatch.setDialog((SIPDialog) evt.getDialog());
					audioBroadcastCatch.setRequest((SIPRequest) evt.getRequest());
					audioBroadcastManager.update(audioBroadcastCatch);
					String waiteStreamTimeoutTaskKey = "waite-stream-" + audioBroadcastCatch.getDeviceId() + audioBroadcastCatch.getChannelId();
					dynamicTask.stop(waiteStreamTimeoutTaskKey);
				}
				logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, " ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
			} else {
				logger.error("RTP推流失败: {}, 参数：{}",jsonObject.getString("msg"),JSONObject.toJSON(param));
				if (sendRtpItem.isOnlyAudio()) {
					// 语音对讲
					try {
						cmder.streamByeCmd((SIPDialog) evt.getDialog(), (SIPRequest)evt.getRequest(), null);
					} catch (SipException e) {
						throw new RuntimeException(e);
					} catch (ParseException e) {
						throw new RuntimeException(e);
					}
				}else {
					// 向上级平台
					commanderForPlatform.streamByeCmd(parentPlatform, callIdHeader.getCallId());
				}
			}


//			if (streamInfo == null) { // 流还没上来，对方就回复ack
//				logger.info("监听流以等待流上线1 rtp/{}", sendRtpItem.getStreamId());
//				// 监听流上线
//				// 添加订阅
//				JSONObject subscribeKey = new JSONObject();
//				subscribeKey.put("app", "rtp");
//				subscribeKey.put("stream", sendRtpItem.getStreamId());
//				subscribeKey.put("regist", true);
//				subscribeKey.put("schema", "rtmp");
//				subscribeKey.put("mediaServerId", sendRtpItem.getMediaServerId());
//				subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey,
//						(MediaServerItem mediaServerItemInUse, JSONObject json)->{
//							Map<String, Object> param = new HashMap<>();
//							param.put("vhost","__defaultVhost__");
//							param.put("app",json.getString("app"));
//							param.put("stream",json.getString("stream"));
//							param.put("ssrc", sendRtpItem.getSsrc());
//							param.put("dst_url",sendRtpItem.getIp());
//							param.put("dst_port", sendRtpItem.getPort());
//							param.put("is_udp", is_Udp);
//							param.put("src_port", sendRtpItem.getLocalPort());
//							zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
//						});
//			}else {
//				Map<String, Object> param = new HashMap<>();
//				param.put("vhost","__defaultVhost__");
//				param.put("app",streamInfo.getApp());
//				param.put("stream",streamInfo.getStream());
//				param.put("ssrc", sendRtpItem.getSsrc());
//				param.put("dst_url",sendRtpItem.getIp());
//				param.put("dst_port", sendRtpItem.getPort());
//				param.put("is_udp", is_Udp);
//				param.put("src_port", sendRtpItem.getLocalPort());
//
//				JSONObject jsonObject = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
//				if (jsonObject.getInteger("code") != 0) {
//					logger.info("监听流以等待流上线2 {}/{}", streamInfo.getApp(), streamInfo.getStream());
//					// 监听流上线
//					// 添加订阅
//					JSONObject subscribeKey = new JSONObject();
//					subscribeKey.put("app", "rtp");
//					subscribeKey.put("stream", streamInfo.getStream());
//					subscribeKey.put("regist", true);
//					subscribeKey.put("schema", "rtmp");
//					subscribeKey.put("mediaServerId", sendRtpItem.getMediaServerId());
//					subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey,
//							(MediaServerItem mediaServerItemInUse, JSONObject json)->{
//								zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
//							});
//				}
//			}
		}
	}
}
