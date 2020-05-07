package com.genersoft.iot.vmp.gb28181.transmit.response;

import javax.sip.ResponseEvent;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;

/**    
 * @Description:处理接收IPCamera发来的SIP协议响应消息
 * @author: songww
 * @date:   2020年5月3日 下午4:42:22     
 */
public interface ISIPResponseProcessor {

	public void process(ResponseEvent evt, SipLayer layer, SipConfig config);

}
