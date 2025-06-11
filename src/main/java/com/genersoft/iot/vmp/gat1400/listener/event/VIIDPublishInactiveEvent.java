package com.genersoft.iot.vmp.gat1400.listener.event;

import org.springframework.context.ApplicationEvent;

import cz.data.viid.framework.domain.entity.VIIDPublish;

public class VIIDPublishInactiveEvent extends ApplicationEvent {

    public VIIDPublishInactiveEvent(VIIDPublish publish) {
        super(publish);
    }

    public VIIDPublish getPublish() {
        return (VIIDPublish) super.getSource();
    }
}
