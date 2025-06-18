package com.genersoft.iot.vmp.gat1400.listener.event;

import org.springframework.context.ApplicationEvent;

public class ServerOfflineEvent extends ApplicationEvent {

    public ServerOfflineEvent(String deviceId) {
        super(deviceId);
    }

    public String getDeviceId() {
        return (String) super.getSource();
    }
}
