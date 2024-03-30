package com.genersoft.iot.vmp.media.event.media;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;

/**
 * RtpServer收流超时事件
 */
public class MediaRtpServerTimeoutEvent extends MediaEvent {
    public MediaRtpServerTimeoutEvent(Object source) {
        super(source);
    }

    public static MediaRtpServerTimeoutEvent getInstance(Object source, OnStreamChangedHookParam hookParam, MediaServer mediaServer){
        MediaRtpServerTimeoutEvent mediaDepartureEven = new MediaRtpServerTimeoutEvent(source);
        mediaDepartureEven.setApp(hookParam.getApp());
        mediaDepartureEven.setStream(hookParam.getStream());
        mediaDepartureEven.setSchema(hookParam.getSchema());
        mediaDepartureEven.setMediaServer(mediaServer);
        return mediaDepartureEven;
    }
}
