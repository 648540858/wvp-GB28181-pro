package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamType;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;
import com.genersoft.iot.vmp.service.impl.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.SerializeUtils;
import org.ehcache.shadow.org.terracotta.offheapstore.storage.IntegerStorageEngine;
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
	private RedisGbPlayMsgListener redisGbPlayMsgListener;


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
			param.put("pt", sendRtpItem.getPt());
			param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
			param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
			if (mediaInfo == null) {
				RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
						sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStreamId(),
						sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
						sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
				redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, jsonObject->{
					startSendRtpStreamHand(evt, sendRtpItem, parentPlatform, jsonObject, param, callIdHeader);
				});
			}else {
				JSONObject jsonObject = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
				startSendRtpStreamHand(evt, sendRtpItem, parentPlatform, jsonObject, param, callIdHeader);
			}


		}
	}
	private void startSendRtpStreamHand(RequestEvent evt, SendRtpItem sendRtpItem, ParentPlatform parentPlatform,
										JSONObject jsonObject, Map<String, Object> param, CallIdHeader callIdHeader) {
		if (jsonObject == null) {
			logger.error("RTP推流失败: 请检查ZLM服务");
		} else if (jsonObject.getInteger("code") == 0) {
			logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, " ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
			byte[] dialogByteArray = SerializeUtils.serialize(evt.getDialog());
			sendRtpItem.setDialog(dialogByteArray);
			byte[] transactionByteArray = SerializeUtils.serialize(evt.getServerTransaction());
			sendRtpItem.setTransaction(transactionByteArray);
			redisCatchStorage.updateSendRTPSever(sendRtpItem);
		} else {
			logger.error("RTP推流失败: {}, 参数：{}",jsonObject.getString("msg"),JSONObject.toJSON(param));
			if (sendRtpItem.isOnlyAudio()) {
				// TODO 可能是语音对讲
			}else {
				// 向上级平台
				commanderForPlatform.streamByeCmd(parentPlatform, callIdHeader.getCallId());
			}
		}
	}
}
