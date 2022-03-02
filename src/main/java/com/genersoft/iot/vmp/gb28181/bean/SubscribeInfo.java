package com.genersoft.iot.vmp.gb28181.bean;

import javax.sip.RequestEvent;
import javax.sip.header.*;
import javax.sip.message.Request;

public class SubscribeInfo {

    public SubscribeInfo() {
    }

    public SubscribeInfo(RequestEvent evt, String id) {
        this.id = id;
        Request request = evt.getRequest();
        CallIdHeader callIdHeader = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
        this.callId = callIdHeader.getCallId();
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        this.fromTag = fromHeader.getTag();
        ExpiresHeader expiresHeader = (ExpiresHeader)request.getHeader(ExpiresHeader.NAME);
        this.expires = expiresHeader.getExpires();
        EventHeader eventHeader = (EventHeader)request.getHeader(EventHeader.NAME);
        this.eventId = eventHeader.getEventId();
        this.eventType = eventHeader.getEventType();
        ViaHeader viaHeader = (ViaHeader)request.getHeader(ViaHeader.NAME);
        this.branch = viaHeader.getBranch();
    }

    private String id;
    private int expires;
    private String callId;
    private String eventId;
    private String eventType;
    private String fromTag;
    private String toTag;
    private String branch;

    public String getId() {
        return id;
    }

    public int getExpires() {
        return expires;
    }

    public String getCallId() {
        return callId;
    }

    public String getFromTag() {
        return fromTag;
    }

    public void setToTag(String toTag) {
        this.toTag = toTag;
    }

    public String getToTag() {
        return toTag;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public void setFromTag(String fromTag) {
        this.fromTag = fromTag;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
