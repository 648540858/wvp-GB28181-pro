package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**    
 * @description: BYE请求响应器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:05     
 */
@Component
public class ByeResponseProcessor extends SIPResponseProcessorAbstract {

	private final String method = "BYE";

	@Autowired
	private SipLayer sipLayer;

	@Autowired
	private SipConfig config;


	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addResponseProcessor(method, this);
	}
	/**
	 * 处理BYE响应
	 * 
	 * @param evt
	 */
	@Override
	public void process(ResponseEvent evt) {
		// TODO Auto-generated method stub
	}


}
