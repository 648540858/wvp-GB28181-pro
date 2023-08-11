package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.InviteSessionType;

public class SsrcTransaction {

    private String deviceId;
    private String channelId;
    private String callId;
    private String stream;
    private String mediaServerId;
    private String ssrc;

    private SipTransactionInfo sipTransactionInfo;

    private InviteSessionType type;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public InviteSessionType getType() {
        return type;
    }

    public void setType(InviteSessionType type) {
        this.type = type;
    }

    public SipTransactionInfo getSipTransactionInfo() {
        return sipTransactionInfo;
    }

    public void setSipTransactionInfo(SipTransactionInfo sipTransactionInfo) {
        this.sipTransactionInfo = sipTransactionInfo;
    }
}
