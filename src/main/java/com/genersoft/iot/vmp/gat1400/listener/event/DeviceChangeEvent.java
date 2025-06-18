package com.genersoft.iot.vmp.gat1400.listener.event;

import org.springframework.context.ApplicationEvent;

public class DeviceChangeEvent extends ApplicationEvent {

    public DeviceChangeEvent(String deviceId) {
        super(deviceId);
    }

    public String getDeviceId() {
        return (String) source;
    }
}
