package com.genersoft.iot.vmp.gb28181.transmit.request;

import javax.sip.RequestEvent;

import com.genersoft.iot.vmp.gb28181.SipLayer;

/**    
 * @Description:处理接收IPCamera发来的SIP协议请求消息
 * @author: swwheihei
 * @date:   2020年5月3日 下午4:42:22     
 */
public interface ISIPRequestProcessor {

	public void process(RequestEvent evt, SipLayer layer);

}
