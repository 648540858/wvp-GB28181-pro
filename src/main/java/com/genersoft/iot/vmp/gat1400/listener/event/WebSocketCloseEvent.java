package com.genersoft.iot.vmp.gat1400.listener.event;

import org.springframework.context.ApplicationEvent;

import cz.data.viid.framework.domain.entity.VIIDServer;

public class WebSocketCloseEvent extends ApplicationEvent {

    public WebSocketCloseEvent(VIIDServer server) {
        super(server);
    }

    public VIIDServer getVIIDServer() {
        return (VIIDServer)super.getSource();
    }
}
