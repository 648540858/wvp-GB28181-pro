package com.genersoft.iot.vmp.gat1400.listener.event;

import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;

import org.springframework.context.ApplicationEvent;


public class WebSocketCloseEvent extends ApplicationEvent {

    public WebSocketCloseEvent(VIIDServer server) {
        super(server);
    }

    public VIIDServer getVIIDServer() {
        return (VIIDServer)super.getSource();
    }
}
