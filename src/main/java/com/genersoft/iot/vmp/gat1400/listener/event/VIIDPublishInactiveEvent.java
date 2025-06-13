package com.genersoft.iot.vmp.gat1400.listener.event;

import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;

import org.springframework.context.ApplicationEvent;


public class VIIDPublishInactiveEvent extends ApplicationEvent {

    public VIIDPublishInactiveEvent(VIIDPublish publish) {
        super(publish);
    }

    public VIIDPublish getPublish() {
        return (VIIDPublish) super.getSource();
    }
}
