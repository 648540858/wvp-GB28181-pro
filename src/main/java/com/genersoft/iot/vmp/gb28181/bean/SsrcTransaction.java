package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;

public class SsrcTransaction {

    private String deviceId;
    private String channelId;
    private String callId;
    private String stream;
    private byte[] transaction;
    private byte[] dialog;
    private String mediaServerId;
    private String ssrc;
    private VideoStreamSessionManager.SessionType type;

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

    public byte[] getTransaction() {
        return transaction;
    }

    public void setTransaction(byte[] transaction) {
        this.transaction = transaction;
    }

    public byte[] getDialog() {
        return dialog;
    }

    public void setDialog(byte[] dialog) {
        this.dialog = dialog;
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

    public VideoStreamSessionManager.SessionType getType() {
        return type;
    }

    public void setType(VideoStreamSessionManager.SessionType type) {
        this.type = type;
    }
}
