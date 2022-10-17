package com.genersoft.iot.vmp.common;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "HTTP-FLV流地址")
    private String flv;

    @Schema(description = "IP")
    private String ip;
    @Schema(description = "HTTPS-FLV流地址")
    private String https_flv;
    @Schema(description = "Websocket-FLV流地址")
    private String ws_flv;
    @Schema(description = "Websockets-FLV流地址")
    private String wss_flv;
    @Schema(description = "HTTP-FMP4流地址")
    private String fmp4;
    @Schema(description = "HTTPS-FMP4流地址")
    private String https_fmp4;
    @Schema(description = "Websocket-FMP4流地址")
    private String ws_fmp4;
    @Schema(description = "Websockets-FMP4流地址")
    private String wss_fmp4;
    @Schema(description = "HLS流地址")
    private String hls;
    @Schema(description = "HTTPS-HLS流地址")
    private String https_hls;
    @Schema(description = "Websocket-HLS流地址")
    private String ws_hls;
    @Schema(description = "Websockets-HLS流地址")
    private String wss_hls;
    @Schema(description = "HTTP-TS流地址")
    private String ts;
    @Schema(description = "HTTPS-TS流地址")
    private String https_ts;
    @Schema(description = "Websocket-TS流地址")
    private String ws_ts;
    @Schema(description = "Websockets-TS流地址")
    private String wss_ts;
    @Schema(description = "RTMP流地址")
    private String rtmp;
    @Schema(description = "RTMPS流地址")
    private String rtmps;
    @Schema(description = "RTSP流地址")
    private String rtsp;
    @Schema(description = "RTSPS流地址")
    private String rtsps;
    @Schema(description = "RTC流地址")
    private String rtc;

    @Schema(description = "RTCS流地址")
    private String rtcs;
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


    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getRtcs() {
        return rtcs;
    }

    public void setRtcs(String rtcs) {
        this.rtcs = rtcs;
    }

    public boolean isPause() {
        return pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }
}
