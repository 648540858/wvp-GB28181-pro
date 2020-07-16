package com.genersoft.iot.vmp.gb28181.transmit.response.impl;

import javax.sip.ResponseEvent;

import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;

/**    
 * @Description:CANCEL响应处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:23     
 */
@Component
public class CancelResponseProcessor implements ISIPResponseProcessor {

	/**   
	 * 处理CANCEL响应
	 *  
	 * @param evt
	 * @param layer
	 * @param transaction
	 * @param config    
	 */  
	@Override
	public void process(ResponseEvent evt, SipLayer layer, SipConfig config) {
		// TODO Auto-generated method stub
		
	}

}
