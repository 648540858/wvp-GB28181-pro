package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.RequestEvent;

import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;

/**    
 * @Description:CANCEL请求处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:23     
 */
public class CancelRequestProcessor extends SIPRequestAbstractProcessor {

	/**   
	 * 处理CANCEL请求
	 *  
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(RequestEvent evt) {
		// TODO 优先级99 Cancel Request消息实现，此消息一般为级联消息，上级给下级发送请求取消指令
		
	}

}
