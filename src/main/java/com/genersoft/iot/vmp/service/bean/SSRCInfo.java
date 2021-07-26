package com.genersoft.iot.vmp.service.bean;

public class SSRCInfo {

    private int port;
    private String ssrc;
    private String StreamId;

    public SSRCInfo(int port, String ssrc, String streamId) {
        this.port = port;
        this.ssrc = ssrc;
        StreamId = streamId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getStreamId() {
        return StreamId;
    }

    public void setStreamId(String streamId) {
        StreamId = streamId;
    }
}
