package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.RequestEvent;

import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**    
 * @Description:暂不支持的消息请求处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:59     
 */
public class OtherRequestProcessor extends SIPRequestAbstractProcessor {

	private Logger logger = LoggerFactory.getLogger(OtherRequestProcessor.class);

	/**   
	 * <p>Title: process</p>   
	 * <p>Description: </p>   
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(RequestEvent evt) {
		logger.info("Unsupported the method: " + evt.getRequest().getMethod());
	}

}
