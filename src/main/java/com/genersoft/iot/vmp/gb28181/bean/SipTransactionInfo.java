package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.message.SIPRequest;

public class SipTransactionInfo {

    private String callId;
    private String fromTag;
    private String toTag;
    private String viaBranch;

    public SipTransactionInfo(SIPRequest request) {
        this.callId = request.getCallIdHeader().getCallId();
        this.fromTag = request.getFromTag();
        this.toTag = request.getToTag();
        this.viaBranch = request.getTopmostViaHeader().getBranch();
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
