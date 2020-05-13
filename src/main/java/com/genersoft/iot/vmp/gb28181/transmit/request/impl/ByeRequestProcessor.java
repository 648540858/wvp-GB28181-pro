package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;

/**    
 * @Description: BYE请求处理器
 * @author: songww
 * @date:   2020年5月3日 下午5:32:05     
 */
@Component
public class ByeRequestProcessor implements ISIPRequestProcessor {

	/**   
	 * 处理BYE请求
	 * 
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(RequestEvent evt, SipLayer layer) {
		// TODO Auto-generated method stub
		
	}

}
