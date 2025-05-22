package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;

@Data
public class SipTransactionInfo {

    private String callId;
    private String fromTag;
    private String toTag;
    private String viaBranch;
    private int expires;
    private String user;

    // 自己是否媒体流发送者
    private boolean asSender;

    public SipTransactionInfo(SIPResponse response, boolean asSender) {
        this.callId = response.getCallIdHeader().getCallId();
        this.fromTag = response.getFromTag();
        this.toTag = response.getToTag();
        this.viaBranch = response.getTopmostViaHeader().getBranch();
        this.asSender = asSender;
    }

    public SipTransactionInfo(SIPResponse response) {
        this.callId = response.getCallIdHeader().getCallId();
        this.fromTag = response.getFromTag();
        this.toTag = response.getToTag();
        this.viaBranch = response.getTopmostViaHeader().getBranch();
        this.asSender = false;
    }

    public SipTransactionInfo() {
    }

}
