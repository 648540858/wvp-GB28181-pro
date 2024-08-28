package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;

import javax.sip.header.*;
import java.util.UUID;

@Data
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


    public static SubscribeInfo buildSimulated(String platFormServerId, String platFormServerIp){
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setId(platFormServerId);
        subscribeInfo.setExpires(-1);
        subscribeInfo.setEventType("Catalog");
        int random = (int) Math.floor(Math.random() * 10000);
        subscribeInfo.setEventId(random + "");
        subscribeInfo.setSimulatedCallId(UUID.randomUUID().toString().replace("-", "") + "@" + platFormServerIp);
        subscribeInfo.setSimulatedFromTag(UUID.randomUUID().toString().replace("-", ""));
        subscribeInfo.setSimulatedToTag(UUID.randomUUID().toString().replace("-", ""));
        return subscribeInfo;
    }

}
