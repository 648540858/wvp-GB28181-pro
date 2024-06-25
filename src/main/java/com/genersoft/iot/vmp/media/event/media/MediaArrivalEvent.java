package com.genersoft.iot.vmp.media.event.media;

import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;

/**
 * 流到来事件
 */
public class MediaArrivalEvent extends MediaEvent {
    public MediaArrivalEvent(Object source) {
        super(source);
    }

    public static MediaArrivalEvent getInstance(Object source, OnStreamChangedHookParam hookParam, MediaServer mediaServer){
        MediaArrivalEvent mediaArrivalEvent = new MediaArrivalEvent(source);
        mediaArrivalEvent.setMediaInfo(MediaInfo.getInstance(hookParam, mediaServer));
        mediaArrivalEvent.setApp(hookParam.getApp());
        mediaArrivalEvent.setStream(hookParam.getStream());
        mediaArrivalEvent.setMediaServer(mediaServer);
        mediaArrivalEvent.setSchema(hookParam.getSchema());
        mediaArrivalEvent.setSchema(hookParam.getSchema());
        mediaArrivalEvent.setHookParam(hookParam);
        return mediaArrivalEvent;
    }

    private MediaInfo mediaInfo;

    private String callId;

    private OnStreamChangedHookParam hookParam;

    private StreamContent streamInfo;

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }


    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public OnStreamChangedHookParam getHookParam() {
        return hookParam;
    }

    public void setHookParam(OnStreamChangedHookParam hookParam) {
        this.hookParam = hookParam;
    }

    public StreamContent getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamContent streamInfo) {
        this.streamInfo = streamInfo;
    }
}
