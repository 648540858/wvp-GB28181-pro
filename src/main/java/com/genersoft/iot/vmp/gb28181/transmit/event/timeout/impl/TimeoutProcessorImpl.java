package com.genersoft.iot.vmp.gb28181.transmit.event.timeout.impl;

import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.timeout.ITimeoutProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.TimeoutEvent;
import javax.sip.header.CallIdHeader;

@Slf4j
@Component
public class TimeoutProcessorImpl implements InitializingBean, ITimeoutProcessor {

    @Autowired
    private SIPProcessorObserver processorObserver;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Override
    public void afterPropertiesSet() throws Exception {
        processorObserver.addTimeoutProcessor(this);
    }

    @Override
    public void process(TimeoutEvent event) {
        try {
            // TODO Auto-generated method stub
            CallIdHeader callIdHeader = event.getClientTransaction().getDialog().getCallId();
            String callId = callIdHeader.getCallId();
            SipEvent sipEvent = sipSubscribe.getSubscribe(callId);
            if (sipEvent != null && sipEvent.getErrorEvent() != null) {
                SipSubscribe.EventResult<TimeoutEvent> timeoutEventEventResult = new SipSubscribe.EventResult<>(event);
                sipEvent.getErrorEvent().response(timeoutEventEventResult);
                sipSubscribe.removeSubscribe(callId);
            }
        } catch (Exception e) {
            log.error("[超时事件失败]: {}", e.getMessage());
        }
    }
}
