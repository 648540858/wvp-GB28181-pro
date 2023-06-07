package com.genersoft.iot.vmp.sip.service;

import javax.sip.ResponseEvent;


public interface ISIPResponseProcessor {

	void process(ResponseEvent evt);


}
