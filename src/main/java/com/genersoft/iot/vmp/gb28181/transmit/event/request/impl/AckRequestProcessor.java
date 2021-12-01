package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
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


	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		Dialog dialog = evt.getDialog();
		if (dialog == null) return;
		if (dialog.getState()== DialogState.CONFIRMED) {
			String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId);
			String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
			String deviceId = sendRtpItem.getDeviceId();
			StreamInfo streamInfo = null;
			if (deviceId == null) {
				streamInfo = new StreamInfo();
				streamInfo.setApp(sendRtpItem.getApp());
				streamInfo.setStreamId(sendRtpItem.getStreamId());
			}else {
				streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
				sendRtpItem.setStreamId(streamInfo.getStreamId());
				streamInfo.setApp("rtp");
			}

			redisCatchStorage.updateSendRTPSever(sendRtpItem);
			logger.info(platformGbId);
			logger.info(channelId);
			Map<String, Object> param = new HashMap<>();
			param.put("vhost","__defaultVhost__");
			param.put("app",streamInfo.getApp());
			param.put("stream",streamInfo.getStreamId());
			param.put("ssrc", sendRtpItem.getSsrc());
			param.put("dst_url",sendRtpItem.getIp());
			param.put("dst_port", sendRtpItem.getPort());
			param.put("is_udp", is_Udp);
			//param.put ("src_port", sendRtpItem.getLocalPort());
			// 设备推流查询，成功后才能转推
			boolean rtpPushed = false;
			long startTime = System.currentTimeMillis();
			while (!rtpPushed) {
				try {
					if (System.currentTimeMillis() - startTime < 30 * 1000) {
						MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
						if (zlmrtpServerFactory.isStreamReady(mediaInfo, streamInfo.getApp(), streamInfo.getStreamId())) {
							rtpPushed = true;
							logger.info("已获取设备推流[{}/{}]，开始向上级推流[{}:{}]",
									streamInfo.getApp() ,streamInfo.getStreamId(), sendRtpItem.getIp(), sendRtpItem.getPort());
							zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
						} else {
							logger.info("等待设备推流[{}/{}].......",
									streamInfo.getApp() ,streamInfo.getStreamId());
							Thread.sleep(1000);
							continue;
						}
					} else {
						rtpPushed = true;
						logger.info("设备推流[{}/{}]超时，终止向上级推流",
								streamInfo.getApp() ,streamInfo.getStreamId());
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
