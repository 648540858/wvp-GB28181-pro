package com.genersoft.iot.vmp.gb28181.event.device;

import org.springframework.context.ApplicationEvent;

import javax.sip.TimeoutEvent;

/**
 * @author lin
 */
public class RequestTimeoutEvent extends ApplicationEvent {
    public RequestTimeoutEvent(Object source) {
        super(source);
    }


    private TimeoutEvent timeoutEvent;

    public TimeoutEvent getTimeoutEvent() {
        return timeoutEvent;
    }

    public void setTimeoutEvent(TimeoutEvent timeoutEvent) {
        this.timeoutEvent = timeoutEvent;
    }
}
