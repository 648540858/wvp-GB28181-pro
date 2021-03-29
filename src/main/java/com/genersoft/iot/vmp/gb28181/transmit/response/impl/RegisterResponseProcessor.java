package com.genersoft.iot.vmp.gb28181.transmit.response.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.RegisterRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Response;

/**    
 * @Description:Register响应处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:23     
 */
@Component
public class RegisterResponseProcessor implements ISIPResponseProcessor {

	private Logger logger = LoggerFactory.getLogger(RegisterRequestProcessor.class);

	@Autowired
	private ISIPCommanderForPlatform sipCommanderForPlatform;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	public RegisterResponseProcessor() {
	}

	/**
	 * 处理Register响应
	 *
 	 * @param evt
	 * @param layer
	 * @param config
	 */
	@Override
	public void process(ResponseEvent evt, SipLayer layer, SipConfig config) {
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
			sipCommanderForPlatform.register(parentPlatform, callId, www, null, null);
		}else if (response.getStatusCode() == 200){
			// 注册/注销成功
			logger.info(String.format("%s %s成功", platformGBId, action));
			redisCatchStorage.delPlatformRegisterInfo(callId);
			parentPlatform.setStatus(true);
			// 取回Expires设置，避免注销过程中被置为0
			ParentPlatform parentPlatformTmp = storager.queryParentPlatById(platformGBId);
			String expires = parentPlatformTmp.getExpires();
			parentPlatform.setExpires(expires);
			storager.updateParentPlatform(parentPlatform);

			redisCatchStorage.updatePlatformRegister(parentPlatform);

			redisCatchStorage.updatePlatformKeepalive(parentPlatform);

			parentPlatformCatch.setParentPlatform(parentPlatform);

			redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
		}
	}

}
