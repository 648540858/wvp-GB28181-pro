package com.genersoft.iot.vmp.media.event;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.springframework.context.ApplicationEvent;

public class MediaServerChangeEvent extends ApplicationEvent {

    public MediaServerChangeEvent(Object source) {
        super(source);
    }

    private MediaServerItem mediaServerItem;

    public MediaServerItem getMediaServerItem() {
        return mediaServerItem;
    }

    public void setMediaServerItem(MediaServerItem mediaServerItem) {
        this.mediaServerItem = mediaServerItem;
    }
}
