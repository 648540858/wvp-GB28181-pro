package com.genersoft.iot.vmp.media.event;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamNotFoundHookParam;
import org.springframework.context.ApplicationEvent;

/**
 * 流未找到
 */
public class MediaNotFoundEvent extends ApplicationEvent {
    public MediaNotFoundEvent(Object source) {
        super(source);
    }

    private String app;

    private String stream;

    private MediaServer mediaServer;

    private String schema;

    public static MediaNotFoundEvent getInstance(Object source, OnStreamNotFoundHookParam hookParam, MediaServer mediaServer){
        MediaNotFoundEvent mediaDepartureEven = new MediaNotFoundEvent(source);
        mediaDepartureEven.setApp(hookParam.getApp());
        mediaDepartureEven.setStream(hookParam.getStream());
        mediaDepartureEven.setSchema(hookParam.getSchema());
        mediaDepartureEven.setMediaServer(mediaServer);
        return mediaDepartureEven;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public MediaServer getMediaServer() {
        return mediaServer;
    }

    public void setMediaServer(MediaServer mediaServer) {
        this.mediaServer = mediaServer;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
