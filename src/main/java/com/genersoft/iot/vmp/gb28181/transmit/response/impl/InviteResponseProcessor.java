package com.genersoft.iot.vmp.gb28181.transmit.response.impl;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;

/**    
 * @Description:处理INVITE响应
 * @author: songww
 * @date:   2020年5月3日 下午4:43:52     
 */
@Component
public class InviteResponseProcessor implements ISIPResponseProcessor {

	/**
	 * 处理invite响应
	 * 
	 * @param request
	 *            响应消息
	 */ 
	@Override
	public void process(ResponseEvent evt, SipLayer layer, SipConfig config) {
		try {
			Dialog dialog = evt.getDialog();
			Request reqAck =dialog.createAck(1L);
			dialog.sendAck(reqAck);
		} catch (InvalidArgumentException | SipException e) {
			e.printStackTrace();
		}
	}

}
