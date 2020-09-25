package com.genersoft.iot.vmp.common;

public class StreamInfo {

    private String ssrc;
    private String flv;
    private String WS_FLV;
    private String RTMP;
    private String HLS;
    private String RTSP;

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getFlv() {
        return flv;
    }

    public void setFlv(String flv) {
        this.flv = flv;
    }

    public String getWS_FLV() {
        return WS_FLV;
    }

    public void setWS_FLV(String WS_FLV) {
        this.WS_FLV = WS_FLV;
    }

    public String getRTMP() {
        return RTMP;
    }

    public void setRTMP(String RTMP) {
        this.RTMP = RTMP;
    }

    public String getHLS() {
        return HLS;
    }

    public void setHLS(String HLS) {
        this.HLS = HLS;
    }

    public String getRTSP() {
        return RTSP;
    }

    public void setRTSP(String RTSP) {
        this.RTSP = RTSP;
    }
}
