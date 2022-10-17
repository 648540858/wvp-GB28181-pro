package com.genersoft.iot.vmp.common;


import org.springframework.util.ObjectUtils;

public class StreamInfo {

    private String app;
    private String stream;
    private String deviceID;
    private String channelId;
    private String ip;

    private String mediaServerId;
    private Object tracks;
    private String startTime;
    private String endTime;
    private double progress;
    private boolean pause;


    private Integer rtmpPort;
    private Integer rtmpSslPort;
    private Integer rtspPort;
    private Integer rtspSSlPort;
    private Integer httpPort;
    private Integer httpSSLPort;



    private String callId;

    public static class TransactionInfo {
        public String callId;
        public String localTag;
        public String remoteTag;
        public String branch;
    }

    private TransactionInfo transactionInfo;

    public String getApp() {
        return app;
    }

    public StreamInfo setApp(String app) {
        this.app = app;
        return this;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public StreamInfo setDeviceID(String deviceID) {
        this.deviceID = deviceID;
        return this;
    }

    public String getChannelId() {
        return channelId;
    }

    public StreamInfo setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public Object getTracks() {
        return tracks;
    }

    public StreamInfo setTracks(Object tracks) {
        this.tracks = tracks;
        return this;
    }

    public String getStream() {
        return stream;
    }

    public StreamInfo setStream(String stream) {
        this.stream = stream;
        return this;
    }

    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    public StreamInfo setTransactionInfo(TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
        return this;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public StreamInfo setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public StreamInfo setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public StreamInfo setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    public double getProgress() {
        return progress;
    }

    public StreamInfo setProgress(double progress) {
        this.progress = progress;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public StreamInfo setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public boolean isPause() {
        return pause;
    }

    public StreamInfo setPause(boolean pause) {
        this.pause = pause;
        return this;
    }

    public StreamInfo setRtmpPort(Integer rtmpPort) {
        this.rtmpPort = rtmpPort;
        return this;
    }

    public StreamInfo setRtmpSslPort(Integer rtmpSslPort) {
        this.rtmpSslPort = rtmpSslPort;
        return this;
    }

    public StreamInfo setRtspPort(Integer rtspPort) {
        this.rtspPort = rtspPort;
        return this;
    }

    public StreamInfo setRtspSSlPort(Integer rtspSSlPort) {
        this.rtspSSlPort = rtspSSlPort;
        return this;
    }

    public StreamInfo setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
        return this;
    }

    public StreamInfo setHttpSSLPort(Integer httpSSLPort) {
        this.httpSSLPort = httpSSLPort;
        return this;
    }

    public String getCallId() {
        return callId;
    }

    public String getCallIdParam() {
        return ObjectUtils.isEmpty(callId) ? "" : "?callId=" + callId;
    }

    public StreamInfo setCallId(String callId) {
        this.callId=callId;
        return this;
    }

    public Integer getRtmpPort() {
        return rtmpPort;
    }

    public Integer getRtmpSslPort() {
        return rtmpSslPort;
    }

    public Integer getRtspPort() {
        return rtspPort;
    }

    public Integer getRtspSSlPort() {
        return rtspSSlPort;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public Integer getHttpSSLPort() {
        return httpSSLPort;
    }

    public String getFlv() {
        return String.format("http://%s:%s/%s/%s.live.flv%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getWs_flv() {
        return String.format("ws://%s:%s/%s/%s.live.flv%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getRtmp() {
        return String.format("rtmp://%s:%s/%s/%s%s", this.ip, this.rtmpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getHls() {
        return String.format("http://%s:%s/%s/%s/hls.m3u8%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getRtsp() {
        return String.format("rtsp://%s:%s/%s/%s%s", this.ip, this.rtspPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getFmp4() {
        return String.format("http://%s:%s/%s/%s.live.mp4%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getWs_fmp4() {
        return String.format("ws://%s:%s/%s/%s.live.mp4%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getWs_hls() {
        return String.format("ws://%s:%s/%s/%s/hls.m3u8%s", this.ip, this.httpPort, app, stream, getCallIdParam());
    }

    public String getTs() {
        return String.format("http://%s:%s/%s/%s.live.ts%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getWs_ts() {
        return String.format("ws://%s:%s/%s/%s.live.ts%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getRtc() {
        return String.format("http://%s:%s/index/api/webrtc?app=%s&stream=%s&type=play%s", this.ip, this.httpPort, this.app, this.stream, this.getCallIdParam());
    }

    public String getHttps_flv() {
        return this.httpSSLPort != 0 ? String.format("https://%s:%s/%s/%s.live.flv%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getWss_flv() {
        return this.httpSSLPort != 0 ? String.format("wss://%s:%s/%s/%s.live.flv%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getWss_fmp4() {
        return this.httpSSLPort != 0 ? String.format("wss://%s:%s/%s/%s.live.mp4%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getWss_hls() {
        return this.httpSSLPort != 0 ? String.format("wss://%s:%s/%s/%s/hls.m3u8%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getWss_ts() {
        return this.httpSSLPort != 0 ? String.format("wss://%s:%s/%s/%s.live.ts%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getRtmps() {
        return this.rtmpSslPort != 0 ? String.format("rtmps://%s:%s/%s/%s%s", this.ip, this.rtmpSslPort, this.app, this.stream, getCallIdParam()) : null;
    }

    public String getRtsps() {
        return this.rtspSSlPort != 0 ? String.format("rtsps://%s:%s/%s/%s%s", this.ip, this.rtspSSlPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getHttps_hls() {
        return this.httpSSLPort != 0 ? String.format("https://%s:%s/%s/%s/hls.m3u8%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getHttps_fmp4() {
        return this.httpSSLPort != 0 ? String.format("https://%s:%s/%s/%s.live.mp4%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getHttps_ts() {
        return this.httpSSLPort != 0 ? String.format("https://%s:%s/%s/%s.live.ts%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

    public String getRtcs() {
        return this.httpSSLPort != 0 ? String.format("https://%s:%s/index/api/webrtc?app=%s&stream=%s&type=play%s", this.ip, this.httpSSLPort, this.app, this.stream, this.getCallIdParam()) : null;
    }

}
