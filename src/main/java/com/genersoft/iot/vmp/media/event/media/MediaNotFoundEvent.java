package com.genersoft.iot.vmp.media.event.media;

import com.genersoft.iot.vmp.media.abl.bean.hook.ABLHookParam;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamNotFoundHookParam;

/**
 * 流未找到
 */
public class MediaNotFoundEvent extends MediaEvent {
    public MediaNotFoundEvent(Object source) {
        super(source);
    }

    public static MediaNotFoundEvent getInstance(Object source, OnStreamNotFoundHookParam hookParam, MediaServer mediaServer){
        MediaNotFoundEvent mediaDepartureEven = new MediaNotFoundEvent(source);
        mediaDepartureEven.setApp(hookParam.getApp());
        mediaDepartureEven.setStream(hookParam.getStream());
        mediaDepartureEven.setSchema(hookParam.getSchema());
        mediaDepartureEven.setMediaServer(mediaServer);
        return mediaDepartureEven;
    }

    public static MediaNotFoundEvent getInstance(Object source, ABLHookParam hookParam, MediaServer mediaServer){
        MediaNotFoundEvent mediaDepartureEven = new MediaNotFoundEvent(source);
        mediaDepartureEven.setApp(hookParam.getApp());
        mediaDepartureEven.setStream(hookParam.getStream());
        mediaDepartureEven.setMediaServer(mediaServer);
        return mediaDepartureEven;
    }
}
