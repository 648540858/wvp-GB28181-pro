package com.genersoft.iot.vmp.sip.bean;

public interface SipEvent {

    void response(int code, String msg, Object data);
}
