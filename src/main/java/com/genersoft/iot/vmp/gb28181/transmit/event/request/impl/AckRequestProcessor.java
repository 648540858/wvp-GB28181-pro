package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
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
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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


	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		logger.info("ACK请求： {}", ((System.currentTimeMillis())));
		Dialog dialog = evt.getDialog();
		if (dialog == null) return;
		if (dialog.getState()== DialogState.CONFIRMED) {
			String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId);
			String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
			String deviceId = sendRtpItem.getDeviceId();
			StreamInfo streamInfo = null;
			if (sendRtpItem.isPlay()) {
				streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
			}else {
				streamInfo = redisCatchStorage.queryPlaybackByDevice(deviceId, channelId);
			}
			System.out.println(JSON.toJSON(streamInfo));
			if (streamInfo == null) {
				streamInfo = new StreamInfo();
				streamInfo.setApp(sendRtpItem.getApp());
				streamInfo.setStream(sendRtpItem.getStreamId());
			}
			redisCatchStorage.updateSendRTPSever(sendRtpItem);
			logger.info(platformGbId);
			logger.info(channelId);
			Map<String, Object> param = new HashMap<>();
			param.put("vhost","__defaultVhost__");
			param.put("app",streamInfo.getApp());
			param.put("stream",streamInfo.getStream());
			param.put("ssrc", sendRtpItem.getSsrc());
			param.put("dst_url",sendRtpItem.getIp());
			param.put("dst_port", sendRtpItem.getPort());
			param.put("is_udp", is_Udp);
			// 设备推流查询，成功后才能转推
			MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
			zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
//			if (zlmrtpServerFactory.isStreamReady(mediaInfo, streamInfo.getApp(), streamInfo.getStreamId())) {
//				logger.info("已获取设备推流[{}/{}]，开始向上级推流[{}:{}]",
//						streamInfo.getApp() ,streamInfo.getStreamId(), sendRtpItem.getIp(), sendRtpItem.getPort());
//				zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
//			} else {
//				// 对hook进行订阅
//				logger.info("等待设备推流[{}/{}].......",
//						streamInfo.getApp(), streamInfo.getStreamId());
//				Timer timer = new Timer();
//				timer.schedule(new TimerTask() {
//					@Override
//					public void run() {
//						logger.info("设备推流[{}/{}]超时，终止向上级推流",
//								finalStreamInfo.getApp() , finalStreamInfo.getStreamId());
//
//					}
//				}, 30*1000L);
//				// 添加订阅
//				JSONObject subscribeKey = new JSONObject();
//				subscribeKey.put("app", "rtp");
//				subscribeKey.put("stream", streamInfo.getStreamId());
//				subscribeKey.put("mediaServerId", streamInfo.getMediaServerId());
//				subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_publish, subscribeKey,
//						(MediaServerItem mediaServerItemInUse, JSONObject json) -> {
//							logger.info("已获取设备推流[{}/{}]，开始向上级推流[{}:{}]",
//									finalStreamInfo.getApp(), finalStreamInfo.getStreamId(), sendRtpItem.getIp(), sendRtpItem.getPort());
//							timer.cancel();
//							zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
//							subscribe.removeSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey);
//						});
//			}


		}
	}
}
