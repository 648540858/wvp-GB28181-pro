package com.genersoft.iot.vmp.gat1400.listener.event;

import com.genersoft.iot.vmp.gat1400.framework.domain.entity.NodeDevice;

import org.springframework.context.ApplicationEvent;


public class ServerOnlineEvent extends ApplicationEvent {

    public ServerOnlineEvent(NodeDevice device) {
        super(device);
    }

    public NodeDevice getVIIDServer() {
        return (NodeDevice)super.getSource();
    }
}
