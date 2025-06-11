package com.genersoft.iot.vmp.gat1400.listener.event;

import org.springframework.context.ApplicationEvent;

import cz.data.viid.framework.domain.entity.NodeDevice;

public class ServerOnlineEvent extends ApplicationEvent {

    public ServerOnlineEvent(NodeDevice device) {
        super(device);
    }

    public NodeDevice getVIIDServer() {
        return (NodeDevice)super.getSource();
    }
}
