package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sip.header.*;

public class SubscribeInfo {


    public SubscribeInfo(SIPRequest request, String id) {
        this.id = id;
        this.request = request;
        this.expires = request.getExpires().getExpires();
        EventHeader eventHeader = (EventHeader)request.getHeader(EventHeader.NAME);
        this.eventId = eventHeader.getEventId();
        this.eventType = eventHeader.getEventType();

    }

    public SubscribeInfo() {
    }

    private String id;

    private SIPRequest request;
    private int expires;
    private String eventId;
    private String eventType;
    private SIPResponse response;

    /**
     * 以下为可选字段
     * @return
     */
    private String sn;
    private int gpsInterval;

    /**
     * 模拟的FromTag
     */
    private String simulatedFromTag;

    /**
     * 模拟的ToTag
     */
    private String simulatedToTag;

    /**
     * 模拟的CallID
     */
    private String simulatedCallId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SIPRequest getRequest() {
        return request;
    }

    public void setRequest(SIPRequest request) {
        this.request = request;
    }

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
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

    public SIPResponse getResponse() {
        return response;
    }

    public void setResponse(SIPResponse response) {
        this.response = response;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public int getGpsInterval() {
        return gpsInterval;
    }

    public void setGpsInterval(int gpsInterval) {
        this.gpsInterval = gpsInterval;
    }

    public String getSimulatedFromTag() {
        return simulatedFromTag;
    }

    public void setSimulatedFromTag(String simulatedFromTag) {
        this.simulatedFromTag = simulatedFromTag;
    }

    public String getSimulatedCallId() {
        return simulatedCallId;
    }

    public void setSimulatedCallId(String simulatedCallId) {
        this.simulatedCallId = simulatedCallId;
    }

    public String getSimulatedToTag() {
        return simulatedToTag;
    }

    public void setSimulatedToTag(String simulatedToTag) {
        this.simulatedToTag = simulatedToTag;
    }
}
