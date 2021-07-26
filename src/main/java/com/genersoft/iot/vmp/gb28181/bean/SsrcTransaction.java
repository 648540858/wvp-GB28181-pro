package com.genersoft.iot.vmp.gb28181.bean;

import javax.sip.message.Request;

public class SsrcTransaction {

    private String deviceId;
    private String channelId;
    private String ssrc;
    private String streamId;
    private byte[] transaction;
    private byte[] dialog;
    private String mediaServerId;

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

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
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
}
