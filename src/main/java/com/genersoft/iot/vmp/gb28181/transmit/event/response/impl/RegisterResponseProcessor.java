package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * @description:Register响应处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:23
 */
@Slf4j
@Component
public class RegisterResponseProcessor extends SIPResponseProcessorAbstract {

	private final String method = "REGISTER";

	@Autowired
	private ISIPCommanderForPlatform sipCommanderForPlatform;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private SipSubscribe sipSubscribe;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addResponseProcessor(method, this);
	}

	/**
	 * 处理Register响应
	 *
 	 * @param evt 事件
	 */
	@Override
	public void process(ResponseEvent evt) {
		SIPResponse response = (SIPResponse)evt.getResponse();
		String callId = response.getCallIdHeader().getCallId();
		long seqNumber = response.getCSeqHeader().getSeqNumber();
		SipEvent subscribe = sipSubscribe.getSubscribe(callId + seqNumber);
		if (subscribe == null || subscribe.getSipTransactionInfo() == null || subscribe.getSipTransactionInfo().getUser() == null) {
			return;
		}

		String action = subscribe.getSipTransactionInfo().getExpires()  > 0 ? "注册" : "注销";
		String platFormServerGbId = subscribe.getSipTransactionInfo().getUser();

		log.info("[国标级联]{} {}响应 {} ", action, response.getStatusCode(), platFormServerGbId);
		Platform platform = platformService.queryPlatformByServerGBId(platFormServerGbId);
		if (platform == null) {
			log.warn("[国标级联]收到 来自{}的 {} 回复 {}, 但是平台信息未查询到!!!", platFormServerGbId, action, response.getStatusCode());
			return;
		}

		if (response.getStatusCode() == Response.UNAUTHORIZED) {
			WWWAuthenticateHeader www = (WWWAuthenticateHeader)response.getHeader(WWWAuthenticateHeader.NAME);
			SipTransactionInfo sipTransactionInfo = new SipTransactionInfo(response);
			try {
				sipCommanderForPlatform.register(platform, sipTransactionInfo, www, null, null, subscribe.getSipTransactionInfo().getExpires()  > 0);
			} catch (SipException | InvalidArgumentException | ParseException e) {
				log.error("[命令发送失败] 国标级联 再次注册: {}", e.getMessage());
			}
		}else if (response.getStatusCode() == Response.OK){
			if (subscribe.getSipTransactionInfo().getExpires()  > 0) {
				SipTransactionInfo sipTransactionInfo = new SipTransactionInfo(response);
				platformService.online(platform, sipTransactionInfo);
			}else {
				platformService.offline(platform);
			}
		}
	}

}
