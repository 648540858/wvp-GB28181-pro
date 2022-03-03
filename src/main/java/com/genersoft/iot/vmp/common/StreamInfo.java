package com.genersoft.iot.vmp.common;

import com.alibaba.fastjson.JSONArray;

public class StreamInfo {

    private String app;
    private String stream;
    private String deviceID;
    private String channelId;
    private String flv;
    private String https_flv;
    private String ws_flv;
    private String wss_flv;
    private String fmp4;
    private String https_fmp4;
    private String ws_fmp4;
    private String wss_fmp4;
    private String hls;
    private String https_hls;
    private String ws_hls;
    private String wss_hls;
    private String ts;
    private String https_ts;
    private String ws_ts;
    private String wss_ts;
    private String rtmp;
    private String rtmps;
    private String rtsp;
    private String rtsps;
    private String rtc;
    private String mediaServerId;
    private Object tracks;

    public static class TransactionInfo{
        public String callId;
        public String localTag;
        public String remoteTag;
        public String branch;
    }

    private TransactionInfo transactionInfo;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

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

    public Object getTracks() {
        return tracks;
    }

    public void setTracks(Object tracks) {
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getRtc() {
        return rtc;
    }

    public void setRtc(String rtc) {
        this.rtc = rtc;
    }

    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    public void setTransactionInfo(TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getHttps_flv() {
        return https_flv;
    }

    public void setHttps_flv(String https_flv) {
        this.https_flv = https_flv;
    }

    public String getWss_flv() {
        return wss_flv;
    }

    public void setWss_flv(String wss_flv) {
        this.wss_flv = wss_flv;
    }

    public String getWss_fmp4() {
        return wss_fmp4;
    }

    public void setWss_fmp4(String wss_fmp4) {
        this.wss_fmp4 = wss_fmp4;
    }

    public String getWss_hls() {
        return wss_hls;
    }

    public void setWss_hls(String wss_hls) {
        this.wss_hls = wss_hls;
    }

    public String getWss_ts() {
        return wss_ts;
    }

    public void setWss_ts(String wss_ts) {
        this.wss_ts = wss_ts;
    }

    public String getRtmps() {
        return rtmps;
    }

    public void setRtmps(String rtmps) {
        this.rtmps = rtmps;
    }

    public String getRtsps() {
        return rtsps;
    }

    public void setRtsps(String rtsps) {
        this.rtsps = rtsps;
    }

    public String getHttps_hls() {
        return https_hls;
    }

    public void setHttps_hls(String https_hls) {
        this.https_hls = https_hls;
    }

    public String getHttps_fmp4() {
        return https_fmp4;
    }

    public void setHttps_fmp4(String https_fmp4) {
        this.https_fmp4 = https_fmp4;
    }

    public String getHttps_ts() {
        return https_ts;
    }

    public void setHttps_ts(String https_ts) {
        this.https_ts = https_ts;
    }
}
