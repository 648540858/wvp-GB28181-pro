package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "流信息")
public class StreamContent {

    @Schema(description = "应用名")
    private String app;

    @Schema(description = "流ID")
    private String stream;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "HTTP-FLV流地址")
    private String flv;

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
    private MediaInfo mediaInfo;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "时长(回放时使用)")
    private Double duration;

    @Schema(description = "文件下载地址（录像下载使用）")
    private DownloadFileInfo downLoadFilePath;

    @Schema(description = "转码后的视频流")
    private StreamContent transcodeStream;

    private double progress;

    @Schema(description = "拉流代理返回的KEY")
    private String key;

    @Schema(description = "使用的WVP ID")
    private String serverId;

    public StreamContent(StreamInfo streamInfo) {
        if (streamInfo == null) {
            return;
        }
        this.app = streamInfo.getApp();
        this.stream = streamInfo.getStream();
        if (streamInfo.getFlv() != null) {
            this.flv = streamInfo.getFlv().getUrl();
        }
        if (streamInfo.getHttps_flv() != null) {
            this.https_flv = streamInfo.getHttps_flv().getUrl();
        }
        if (streamInfo.getWs_flv() != null) {
            this.ws_flv = streamInfo.getWs_flv().getUrl();
        }
        if (streamInfo.getWss_flv() != null) {
            this.wss_flv = streamInfo.getWss_flv().getUrl();
        }
        if (streamInfo.getFmp4() != null) {
            this.fmp4 = streamInfo.getFmp4().getUrl();
        }
        if (streamInfo.getHttps_fmp4() != null) {
            this.https_fmp4 = streamInfo.getHttps_fmp4().getUrl();
        }
        if (streamInfo.getWs_fmp4() != null) {
            this.ws_fmp4 = streamInfo.getWs_fmp4().getUrl();
        }
        if (streamInfo.getWss_fmp4() != null) {
            this.wss_fmp4 = streamInfo.getWss_fmp4().getUrl();
        }
        if (streamInfo.getHls() != null) {
            this.hls = streamInfo.getHls().getUrl();
        }
        if (streamInfo.getHttps_hls() != null) {
            this.https_hls = streamInfo.getHttps_hls().getUrl();
        }
        if (streamInfo.getWs_hls() != null) {
            this.ws_hls = streamInfo.getWs_hls().getUrl();
        }
        if (streamInfo.getWss_hls() != null) {
            this.wss_hls = streamInfo.getWss_hls().getUrl();
        }
        if (streamInfo.getTs() != null) {
            this.ts = streamInfo.getTs().getUrl();
        }
        if (streamInfo.getHttps_ts() != null) {
            this.https_ts = streamInfo.getHttps_ts().getUrl();
        }
        if (streamInfo.getWs_ts() != null) {
            this.ws_ts = streamInfo.getWs_ts().getUrl();
        }
        if (streamInfo.getRtmp() != null) {
            this.rtmp = streamInfo.getRtmp().getUrl();
        }
        if (streamInfo.getRtmps() != null) {
            this.rtmps = streamInfo.getRtmps().getUrl();
        }
        if (streamInfo.getRtsp() != null) {
            this.rtsp = streamInfo.getRtsp().getUrl();
        }
        if (streamInfo.getRtsps() != null) {
            this.rtsps = streamInfo.getRtsps().getUrl();
        }
        if (streamInfo.getRtc() != null) {
            this.rtc = streamInfo.getRtc().getUrl();
        }
        if (streamInfo.getRtcs() != null) {
            this.rtcs = streamInfo.getRtcs().getUrl();
        }
        if (streamInfo.getMediaServer() != null) {
            this.mediaServerId = streamInfo.getMediaServer().getId();
        }

        this.mediaInfo = streamInfo.getMediaInfo();
        this.startTime = streamInfo.getStartTime();
        this.endTime = streamInfo.getEndTime();
        this.progress = streamInfo.getProgress();
        this.duration = streamInfo.getDuration();
        this.key = streamInfo.getKey();
        this.serverId = streamInfo.getServerId();

        if (streamInfo.getDownLoadFilePath() != null) {
            this.downLoadFilePath = streamInfo.getDownLoadFilePath();
        }
        if (streamInfo.getTranscodeStream() != null) {
            this.transcodeStream = new StreamContent(streamInfo.getTranscodeStream());
        }
    }

}
