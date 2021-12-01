package com.genersoft.iot.vmp.gb28181.transmit.event.request;

import javax.sip.RequestEvent;

/**
 * @description: 对SIP事件进行处理，包括request， response， timeout， ioException, transactionTerminated,dialogTerminated
 * @author: panlinlin
 * @date:   2021年11月5日 15：47
 */
public interface ISIPRequestProcessor {

	void process(RequestEvent event);

}
