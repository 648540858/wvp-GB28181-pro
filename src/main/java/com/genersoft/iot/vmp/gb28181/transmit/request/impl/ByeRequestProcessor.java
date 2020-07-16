package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.RequestEvent;

import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;

/**    
 * @Description: BYE请求处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:05     
 */
public class ByeRequestProcessor extends SIPRequestAbstractProcessor {

	/**   
	 * 处理BYE请求
	 * 
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(RequestEvent evt) {
		// TODO 优先级99 Bye Request消息实现，此消息一般为级联消息，上级给下级发送视频停止指令
		
	}

}
