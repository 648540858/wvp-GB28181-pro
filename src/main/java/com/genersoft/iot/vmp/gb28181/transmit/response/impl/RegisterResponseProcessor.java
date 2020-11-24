package com.genersoft.iot.vmp.gb28181.transmit.response.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.RegisterRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import gov.nist.core.Host;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.To;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ToHeader;
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

	/**
	 * 处理Register响应
	 *
 	 * @param evt
	 * @param layer
	 * @param config
	 */
	@Override
	public void process(ResponseEvent evt, SipLayer layer, SipConfig config) {
		// TODO Auto-generated method stub
		Response response = evt.getResponse();
		ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
		SipUri uri = (SipUri)toHeader.getAddress().getURI();
		String platformGBId = uri.getAuthority().getUser();
		logger.info(String.format("收到 %s 的注册%S请求", platformGBId, response.getStatusCode() ));

		ParentPlatform parentPlatform = storager.queryParentPlatById(platformGBId);
		if (parentPlatform == null) {
			logger.warn(String.format("收到 %s 的注册%S请求, 但是平台信息未查询到!!!", platformGBId, response.getStatusCode()));
			return;
		}

		if (response.getStatusCode() == 401) {

			WWWAuthenticateHeader www = (WWWAuthenticateHeader)response.getHeader(WWWAuthenticateHeader.NAME);
			String realm = www.getRealm();
			String nonce = www.getNonce();
			String scheme = www.getScheme();

			CallIdHeader callIdHeader = (CallIdHeader)response.getHeader(CallIdHeader.NAME);
			String callId = callIdHeader.getCallId();
			sipCommanderForPlatform.register(parentPlatform, callId, realm, nonce, scheme);
		}else if (response.getStatusCode() == 200){
			// 注册成功
			logger.info(String.format("%s 注册成功", platformGBId ));
			parentPlatform.setStatus(true);
			storager.updateParentPlatform(parentPlatform);
		}
	}

}
