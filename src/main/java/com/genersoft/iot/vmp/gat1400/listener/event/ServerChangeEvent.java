package com.genersoft.iot.vmp.gat1400.listener.event;

import org.springframework.context.ApplicationEvent;

public class ServerChangeEvent extends ApplicationEvent {

    public ServerChangeEvent(String serverId) {
        super(serverId);
    }

    public String getServerId() {
        return (String) super.getSource();
    }
}
