package com.genersoft.iot.vmp.sip.service.Impl.response;

import com.genersoft.iot.vmp.sip.SipCommander;
import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import com.genersoft.iot.vmp.sip.service.ISIPResponseProcessor;
import com.genersoft.iot.vmp.sip.service.ISipService;
import com.genersoft.iot.vmp.sip.service.SipSdk;
import com.genersoft.iot.vmp.sip.utils.SipUtils;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

/**    
 * Register响应处理器
 */
@Component
public class SipRegisterResponseProcessor implements ISIPResponseProcessor {

	private final Logger logger = LoggerFactory.getLogger(SipRegisterResponseProcessor.class);

	@Autowired
	private SipCommander sipCommander;

	@Autowired
	private ISipService sipService;

	@Autowired
	private SipSdk sipSdk;



	/**
	 * 处理Register响应
	 *
 	 * @param evt 事件
	 */
	@Override
	public void process(ResponseEvent evt) {
		SIPResponse response = (SIPResponse) evt.getResponse();
		if (response.getStatusCode() == 401) {
			WWWAuthenticateHeader authenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
			String removeHost = response.getRemoteAddress().getHostAddress();
			int remotePort = response.getRemotePort();
			SipServer server = sipService.getSipServerByServerAddress(removeHost, remotePort);
			String username = SipUtils.getUserFromToHeader(response.getToHeader());
			SipServerAccount account = sipService.getAccountByUsername(server.getId(), username);
			SipProviderImpl sipProvider = sipSdk.getProvider(removeHost, remotePort);
			try {
				sipCommander.register(server, account, sipProvider, authenticateHeader, true, (code1, msg1, data1)->{



				}, (code2, msg2, data2)->{

				});
			} catch (InvalidArgumentException | SipException | ParseException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
