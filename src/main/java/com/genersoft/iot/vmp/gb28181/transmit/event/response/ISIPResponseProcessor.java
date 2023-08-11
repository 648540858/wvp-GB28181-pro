package com.genersoft.iot.vmp.gb28181.transmit.event.response;

import javax.sip.ResponseEvent;

/**    
 * @description:处理接收IPCamera发来的SIP协议响应消息
 * @author: swwheihei
 * @date:   2020年5月3日 下午4:42:22     
 */
public interface ISIPResponseProcessor {

	void process(ResponseEvent evt);


}
