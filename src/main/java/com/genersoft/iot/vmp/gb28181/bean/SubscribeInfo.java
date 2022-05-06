package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.utils.SerializeUtils;

import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.*;
import javax.sip.message.Request;

public class SubscribeInfo {


    public SubscribeInfo(RequestEvent evt, String id) {
        this.id = id;
        Request request = evt.getRequest();
        ExpiresHeader expiresHeader = (ExpiresHeader)request.getHeader(ExpiresHeader.NAME);
        this.expires = expiresHeader.getExpires();
        EventHeader eventHeader = (EventHeader)request.getHeader(EventHeader.NAME);
        this.eventId = eventHeader.getEventId();
        this.eventType = eventHeader.getEventType();
        this.transaction = evt.getServerTransaction();
        this.dialog = evt.getDialog();
        CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
        this.callId = callIdHeader.getCallId();
    }

    public SubscribeInfo() {
    }

    private String id;
    private int expires;
    private String callId;
    private String eventId;
    private String eventType;
    private ServerTransaction transaction;
    private Dialog dialog;

    /**
     * 以下为可选字段
     * @return
     */
    private String sn;
    private int gpsInterval;


    public String getId() {
        return id;
    }

    public int getExpires() {
        return expires;
    }

    public String getCallId() {
        return callId;
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

    public ServerTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(ServerTransaction transaction) {
        this.transaction = transaction;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
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
}
