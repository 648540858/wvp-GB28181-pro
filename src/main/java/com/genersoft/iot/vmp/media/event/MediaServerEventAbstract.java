package com.genersoft.iot.vmp.media.event;

import org.springframework.context.ApplicationEvent;

public abstract class MediaServerEventAbstract extends ApplicationEvent {


    private static final long serialVersionUID = 1L;

    private String mediaServerId;


    public MediaServerEventAbstract(Object source) {
        super(source);
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }
}
