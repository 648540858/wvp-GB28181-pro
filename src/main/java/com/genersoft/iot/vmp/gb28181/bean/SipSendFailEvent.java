package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import lombok.Data;

@Data
public class SipSendFailEvent extends SipSubscribe.EventResult<String> {

    private String callId;

    private String msg;

    public static SipSendFailEvent getInstance(String callId, String msg){
        SipSendFailEvent sipSendFailEvent = new SipSendFailEvent();
        sipSendFailEvent.setMsg(msg);
        sipSendFailEvent.setCallId(callId);
        return sipSendFailEvent;
    }
}
