package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

public class SipTransactionInfo {

    private String callId;
    private String fromTag;
    private String toTag;
    private String viaBranch;

    public SipTransactionInfo(SIPResponse response) {
        this.callId = response.getCallIdHeader().getCallId();
        this.fromTag = response.getFromTag();
        this.toTag = response.getToTag();
        this.viaBranch = response.getTopmostViaHeader().getBranch();
    }

    public SipTransactionInfo() {
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getFromTag() {
        return fromTag;
    }

    public void setFromTag(String fromTag) {
        this.fromTag = fromTag;
    }

    public String getToTag() {
        return toTag;
    }

    public void setToTag(String toTag) {
        this.toTag = toTag;
    }

    public String getViaBranch() {
        return viaBranch;
    }

    public void setViaBranch(String viaBranch) {
        this.viaBranch = viaBranch;
    }
}
