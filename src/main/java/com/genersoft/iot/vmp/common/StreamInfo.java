package com.genersoft.iot.vmp.common;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.util.ObjectUtils;

@Schema(description = "流信息")
public class StreamInfo {

    @Schema(description = "应用名")
    private String app;
    @Schema(description = "流ID")
    private String stream;
    @Schema(description = "设备编号")
    private String deviceID;
    @Schema(description = "通道编号")
    private String channelId;

    @Schema(description = "IP")
    private String ip;
    @Schema(description = "内网IP")
    private String intranetIp;

//    @Schema(description = "HTTP-FLV流地址")
//    private String flv;
//    @Schema(description = "HTTPS-FLV流地址")
//    private String https_flv;
//    @Schema(description = "Websocket-FLV流地址")
//    private String ws_flv;
//    @Schema(description = "Websockets-FLV流地址")
//    private String wss_flv;
//    @Schema(description = "HTTP-FMP4流地址")
//    private String fmp4;
//    @Schema(description = "HTTPS-FMP4流地址")
//    private String https_fmp4;
//    @Schema(description = "Websocket-FMP4流地址")
//    private String ws_fmp4;
//    @Schema(description = "Websockets-FMP4流地址")
//    private String wss_fmp4;
//    @Schema(description = "HLS流地址")
//    private String hls;
//    @Schema(description = "HTTPS-HLS流地址")
//    private String https_hls;
//    @Schema(description = "Websocket-HLS流地址")
//    private String ws_hls;
//    @Schema(description = "Websockets-HLS流地址")
//    private String wss_hls;
//    @Schema(description = "HTTP-TS流地址")
//    private String ts;
//    @Schema(description = "HTTPS-TS流地址")
//    private String https_ts;
//    @Schema(description = "Websocket-TS流地址")
//    private String ws_ts;
//    @Schema(description = "Websockets-TS流地址")
//    private String wss_ts;
//    @Schema(description = "RTMP流地址")
//    private String rtmp;
//    @Schema(description = "RTMPS流地址")
//    private String rtmps;
//    @Schema(description = "RTSP流地址")
//    private String rtsp;
//    @Schema(description = "RTSPS流地址")
//    private String rtsps;
//    @Schema(description = "RTC流地址")
//    private String rtc;
//    @Schema(description = "RTCS流地址")
//    private String rtcs;

    @Schema(description = "流媒体ID")
    private String mediaServerId;
    @Schema(description = "流编码信息")
    private Object tracks;
    @Schema(description = "开始时间")
    private String startTime;
    @Schema(description = "结束时间")
    private String endTime;
    @Schema(description = "进度（录像下载使用）")
    private double progress;
    @Schema(description = "是否暂停（录像回放使用）")
    private boolean pause;

    @Schema(description = "RTMP端口")
    private Integer rtmpPort;
    @Schema(description = "RTMPS端口")
    private Integer rtmpSslPort;
    @Schema(description = "RTSP端口")
    private Integer rtspPort;
    @Schema(description = "RTSPS端口")
    private Integer rtspSSlPort;
    @Schema(description = "HTTP端口")
    private Integer httpPort;
    @Schema(description = "HTTPS端口")
    private Integer httpSSLPort;
    @Schema(description = "callId")
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

    public String getIntranetIp() {
        return intranetIp;
    }

    public StreamInfo setIntranetIp(String intranetIp) {
        this.intranetIp = intranetIp;
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
        this.callId = callId;
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

    public String getFmp4ByIntranet() {
        return String.format("http://%s:%s/%s/%s.live.mp4%s", this.intranetIp, this.httpPort, this.app, this.stream, this.getCallIdParam());
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
