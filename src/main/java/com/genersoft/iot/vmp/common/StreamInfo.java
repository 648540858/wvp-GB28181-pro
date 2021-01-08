package com.genersoft.iot.vmp.common;

import com.alibaba.fastjson.JSONArray;

public class StreamInfo {

    private String streamId;
    private String deviceID;
    private String channelId;
    private String flv;
    private String ws_flv;
    private String fmp4;
    private String ws_fmp4;
    private String hls;
    private String ws_hls;
    private String ts;
    private String ws_ts;
    private String rtmp;
    private String rtsp;
    private JSONArray tracks;

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getFlv() {
        return flv;
    }

    public void setFlv(String flv) {
        this.flv = flv;
    }

    public String getWs_flv() {
        return ws_flv;
    }

    public void setWs_flv(String ws_flv) {
        this.ws_flv = ws_flv;
    }

    public String getRtmp() {
        return rtmp;
    }

    public void setRtmp(String rtmp) {
        this.rtmp = rtmp;
    }

    public String getHls() {
        return hls;
    }

    public void setHls(String hls) {
        this.hls = hls;
    }

    public String getRtsp() {
        return rtsp;
    }

    public void setRtsp(String rtsp) {
        this.rtsp = rtsp;
    }

    public JSONArray getTracks() {
        return tracks;
    }

    public void setTracks(JSONArray tracks) {
        this.tracks = tracks;
    }

    public String getFmp4() {
        return fmp4;
    }

    public void setFmp4(String fmp4) {
        this.fmp4 = fmp4;
    }

    public String getWs_fmp4() {
        return ws_fmp4;
    }

    public void setWs_fmp4(String ws_fmp4) {
        this.ws_fmp4 = ws_fmp4;
    }

    public String getWs_hls() {
        return ws_hls;
    }

    public void setWs_hls(String ws_hls) {
        this.ws_hls = ws_hls;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public String getWs_ts() {
        return ws_ts;
    }

    public void setWs_ts(String ws_ts) {
        this.ws_ts = ws_ts;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
}
