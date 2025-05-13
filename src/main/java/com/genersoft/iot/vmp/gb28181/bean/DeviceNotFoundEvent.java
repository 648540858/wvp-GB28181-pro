package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

import javax.sip.Dialog;
import java.util.EventObject;

@Data
public class DeviceNotFoundEvent {

    private String callId;

    public DeviceNotFoundEvent(String callId) {
        this.callId = callId;
    }
}
