package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Response;

/**    
 * @description:Register响应处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:23     
 */
@Component
public class RegisterResponseProcessor extends SIPResponseProcessorAbstract {

	private Logger logger = LoggerFactory.getLogger(RegisterResponseProcessor.class);
	private final String method = "REGISTER";

	@Autowired
	private ISIPCommanderForPlatform sipCommanderForPlatform;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private SubscribeHolder subscribeHolder;

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
		Response response = evt.getResponse();
		CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
		String callId = callIdHeader.getCallId();

		String platformGBId = redisCatchStorage.queryPlatformRegisterInfo(callId);
		if (platformGBId == null) {
			logger.info(String.format("未找到callId： %s 的注册/注销平台id", callId ));
			return;
		}

		ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(platformGBId);
		if (parentPlatformCatch == null) {
			logger.warn(String.format("收到 %s 的注册/注销%S请求, 但是平台缓存信息未查询到!!!", platformGBId, response.getStatusCode()));
			return;
		}
		String action = parentPlatformCatch.getParentPlatform().getExpires().equals("0") ? "注销" : "注册";
		logger.info(String.format("收到 %s %s的%S响应", platformGBId, action, response.getStatusCode() ));
		ParentPlatform parentPlatform = parentPlatformCatch.getParentPlatform();
		if (parentPlatform == null) {
			logger.warn(String.format("收到 %s %s的%S请求, 但是平台信息未查询到!!!", platformGBId, action, response.getStatusCode()));
			return;
		}

		if (response.getStatusCode() == 401) {
			WWWAuthenticateHeader www = (WWWAuthenticateHeader)response.getHeader(WWWAuthenticateHeader.NAME);
			sipCommanderForPlatform.register(parentPlatform, callId, www, null, null, true);
		}else if (response.getStatusCode() == 200){
			// 注册/注销成功
			logger.info(String.format("%s %s成功", platformGBId, action));
			redisCatchStorage.delPlatformRegisterInfo(callId);
			redisCatchStorage.delPlatformCatchInfo(platformGBId);
			// 取回Expires设置，避免注销过程中被置为0
			ParentPlatform parentPlatformTmp = storager.queryParentPlatByServerGBId(platformGBId);
			if (parentPlatformTmp != null) {
				parentPlatformTmp.setStatus("注册".equals(action));
				redisCatchStorage.updatePlatformRegister(parentPlatformTmp);
				redisCatchStorage.updatePlatformKeepalive(parentPlatformTmp);
				parentPlatformCatch.setParentPlatform(parentPlatformTmp);
			}
			redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
			storager.updateParentPlatformStatus(platformGBId, "注册".equals(action));
			if ("注销".equals(action)) {
				subscribeHolder.removeCatalogSubscribe(platformGBId);
				subscribeHolder.removeMobilePositionSubscribe(platformGBId);
			}
		}
	}

}
