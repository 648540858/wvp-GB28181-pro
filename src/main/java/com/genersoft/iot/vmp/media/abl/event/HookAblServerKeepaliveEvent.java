package com.genersoft.iot.vmp.media.abl.event;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import org.springframework.context.ApplicationEvent;

/**
 * zlm 心跳事件
 */
public class HookAblServerKeepaliveEvent extends ApplicationEvent {

    public HookAblServerKeepaliveEvent(Object source) {
        super(source);
    }

    private MediaServer mediaServerItem;

    public MediaServer getMediaServerItem() {
        return mediaServerItem;
    }

    public void setMediaServerItem(MediaServer mediaServerItem) {
        this.mediaServerItem = mediaServerItem;
    }
}
