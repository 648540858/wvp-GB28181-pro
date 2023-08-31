package com.genersoft.iot.vmp.service.bean;

public class SSRCInfo {

    private int port;
    private String ssrc;
    private String Stream;

    public SSRCInfo(int port, String ssrc, String stream) {
        this.port = port;
        this.ssrc = ssrc;
        Stream = stream;
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

    public String getStream() {
        return Stream;
    }

    public void setStream(String stream) {
        Stream = stream;
    }
}
