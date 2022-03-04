package com.genersoft.iot.vmp.service.bean;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

import javax.sip.RequestEvent;

public class PlayBackResult<T> {
     private int code;
     private T data;
     private MediaServerItem mediaServerItem;
    private JSONObject response;
    private SipSubscribe.EventResult event;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public MediaServerItem getMediaServerItem() {
        return mediaServerItem;
    }

    public void setMediaServerItem(MediaServerItem mediaServerItem) {
        this.mediaServerItem = mediaServerItem;
    }

    public JSONObject getResponse() {
        return response;
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }

    public SipSubscribe.EventResult getEvent() {
        return event;
    }

    public void setEvent(SipSubscribe.EventResult event) {
        this.event = event;
    }
}
