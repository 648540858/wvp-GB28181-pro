package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import java.util.*;

/**
 * SIP命令类型： ACK请求
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
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private ZLMHttpHookSubscribe subscribe;

	@Autowired
	private DynamicTask dynamicTask;


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
			// 取消设置的超时任务
			dynamicTask.stop(callIdHeader.getCallId());
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId, null, callIdHeader.getCallId());
			String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
			MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
			logger.info("收到ACK，开始向上级推流 rtp/{}", sendRtpItem.getStreamId());
			Map<String, Object> param = new HashMap<>();
			param.put("vhost","__defaultVhost__");
			param.put("app",sendRtpItem.getApp());
			param.put("stream",sendRtpItem.getStreamId());
			param.put("ssrc", sendRtpItem.getSsrc());
			param.put("dst_url",sendRtpItem.getIp());
			param.put("dst_port", sendRtpItem.getPort());
			param.put("is_udp", is_Udp);
			param.put("src_port", sendRtpItem.getLocalPort());
			zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);



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
