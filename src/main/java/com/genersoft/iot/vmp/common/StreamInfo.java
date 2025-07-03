package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
@Schema(description = "流信息")
public class StreamInfo implements Serializable, Cloneable{

    @Schema(description = "应用名")
    private String app;
    @Schema(description = "流ID")
    private String stream;
    @Schema(description = "设备编号")
    private String deviceId;
    @Schema(description = "通道ID")
    private Integer channelId;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "HTTP-FLV流地址")
    private StreamURL flv;

    @Schema(description = "HTTPS-FLV流地址")
    private StreamURL https_flv;
    @Schema(description = "Websocket-FLV流地址")
    private StreamURL ws_flv;
    @Schema(description = "Websockets-FLV流地址")
    private StreamURL wss_flv;
    @Schema(description = "HTTP-FMP4流地址")
    private StreamURL fmp4;
    @Schema(description = "HTTPS-FMP4流地址")
    private StreamURL https_fmp4;
    @Schema(description = "Websocket-FMP4流地址")
    private StreamURL ws_fmp4;
    @Schema(description = "Websockets-FMP4流地址")
    private StreamURL wss_fmp4;
    @Schema(description = "HLS流地址")
    private StreamURL hls;
    @Schema(description = "HTTPS-HLS流地址")
    private StreamURL https_hls;
    @Schema(description = "Websocket-HLS流地址")
    private StreamURL ws_hls;
    @Schema(description = "Websockets-HLS流地址")
    private StreamURL wss_hls;
    @Schema(description = "HTTP-TS流地址")
    private StreamURL ts;
    @Schema(description = "HTTPS-TS流地址")
    private StreamURL https_ts;
    @Schema(description = "Websocket-TS流地址")
    private StreamURL ws_ts;
    @Schema(description = "Websockets-TS流地址")
    private StreamURL wss_ts;
    @Schema(description = "RTMP流地址")
    private StreamURL rtmp;
    @Schema(description = "RTMPS流地址")
    private StreamURL rtmps;
    @Schema(description = "RTSP流地址")
    private StreamURL rtsp;
    @Schema(description = "RTSPS流地址")
    private StreamURL rtsps;
    @Schema(description = "RTC流地址")
    private StreamURL rtc;

    @Schema(description = "RTCS流地址")
    private StreamURL rtcs;
    @Schema(description = "流媒体节点")
    private MediaServer mediaServer;
    @Schema(description = "流编码信息")
    private MediaInfo mediaInfo;
    @Schema(description = "开始时间")
    private String startTime;
    @Schema(description = "结束时间")
    private String endTime;
    @Schema(description = "进度（录像下载使用）")
    private double progress;
    @Schema(description = "文件下载地址（录像下载使用）")
    private DownloadFileInfo downLoadFilePath;
    @Schema(description = "点播请求的callId")
    private String callId;

    @Schema(description = "是否暂停（录像回放使用）")
    private boolean pause;

    @Schema(description = "产生源类型，包括 unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7")
    private int originType;

    @Schema(description = "originType的文本描述")
    private String originTypeStr;

    @Schema(description = "转码后的视频流")
    private StreamInfo transcodeStream;

    @Schema(description = "使用的WVP ID")
    private String serverId;

    public void setRtmp(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s%s", app, stream, callIdParam);
        if (port > 0) {
            this.rtmp = new StreamURL("rtmp", host, port, file);
        }
        if (sslPort > 0) {
            this.rtmps = new StreamURL("rtmps", host, sslPort, file);
        }
    }

    public void setRtsp(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s%s", app, stream, callIdParam);
        if (port > 0) {
            this.rtsp = new StreamURL("rtsp", host, port, file);
        }
        if (sslPort > 0) {
            this.rtsps = new StreamURL("rtsps", host, sslPort, file);
        }
    }

    public void setFlv(String host, int port, int sslPort, String file) {
        if (port > 0) {
            this.flv = new StreamURL("http", host, port, file);
        }
        this.ws_flv = new StreamURL("ws", host, port, file);
        if (sslPort > 0) {
            this.https_flv = new StreamURL("https", host, sslPort, file);
            this.wss_flv = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setWsFlv(String host, int port, int sslPort, String file) {
        if (port > 0) {
            this.ws_flv = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.wss_flv = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setFmp4(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.mp4%s", app, stream, callIdParam);
        if (port > 0) {
            this.fmp4 = new StreamURL("http", host, port, file);
            this.ws_fmp4 = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.https_fmp4 = new StreamURL("https", host, sslPort, file);
            this.wss_fmp4 = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setHls(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s/hls.m3u8%s", app, stream, callIdParam);
        if (port > 0) {
            this.hls = new StreamURL("http", host, port, file);
            this.ws_hls = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.https_hls = new StreamURL("https", host, sslPort, file);
            this.wss_hls = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setTs(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.ts%s", app, stream, callIdParam);

        if (port > 0) {
            this.ts = new StreamURL("http", host, port, file);
            this.ws_ts = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.https_ts = new StreamURL("https", host, sslPort, file);
            this.wss_ts = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setRtc(String host, int port, int sslPort, String app, String stream, String callIdParam, boolean isPlay) {
        if (callIdParam != null) {
            callIdParam = Objects.equals(callIdParam, "") ? callIdParam : callIdParam.replace("?", "&");
        }
        String file = String.format("index/api/webrtc?app=%s&stream=%s&type=%s%s", app, stream, isPlay?"play":"push", callIdParam);
        if (port > 0) {
            this.rtc = new StreamURL("http", host, port, file);
        }
        if (sslPort > 0) {
            this.rtcs = new StreamURL("https", host, sslPort, file);
        }
    }

    public void changeStreamIp(String localAddr) {
        if (this.flv != null) {
            this.flv.setHost(localAddr);
        }
        if (this.ws_flv != null ){
            this.ws_flv.setHost(localAddr);
        }
        if (this.hls != null ) {
            this.hls.setHost(localAddr);
        }
        if (this.ws_hls != null ) {
            this.ws_hls.setHost(localAddr);
        }
        if (this.ts != null ) {
            this.ts.setHost(localAddr);
        }
        if (this.ws_ts != null ) {
            this.ws_ts.setHost(localAddr);
        }
        if (this.fmp4 != null ) {
            this.fmp4.setHost(localAddr);
        }
        if (this.ws_fmp4 != null ) {
            this.ws_fmp4.setHost(localAddr);
        }
        if (this.rtc != null ) {
            this.rtc.setHost(localAddr);
        }
        if (this.https_flv != null) {
            this.https_flv.setHost(localAddr);
        }
        if (this.wss_flv != null) {
            this.wss_flv.setHost(localAddr);
        }
        if (this.https_hls != null) {
            this.https_hls.setHost(localAddr);
        }
        if (this.wss_hls != null) {
            this.wss_hls.setHost(localAddr);
        }
        if (this.wss_ts != null) {
            this.wss_ts.setHost(localAddr);
        }
        if (this.https_fmp4 != null) {
            this.https_fmp4.setHost(localAddr);
        }
        if (this.wss_fmp4 != null) {
            this.wss_fmp4.setHost(localAddr);
        }
        if (this.rtcs != null) {
            this.rtcs.setHost(localAddr);
        }
        if (this.rtsp != null) {
            this.rtsp.setHost(localAddr);
        }
        if (this.rtsps != null) {
            this.rtsps.setHost(localAddr);
        }
        if (this.rtmp != null) {
            this.rtmp.setHost(localAddr);
        }
        if (this.rtmps != null) {
            this.rtmps.setHost(localAddr);
        }
    }


    public static class TransactionInfo{
        public String callId;
        public String localTag;
        public String remoteTag;
        public String branch;
    }

    private TransactionInfo transactionInfo;


    @Override
    public StreamInfo clone() {
        StreamInfo instance = null;
        try{
            instance = (StreamInfo)super.clone();
            if (this.flv != null) {
                instance.flv=this.flv.clone();
            }
            if (this.ws_flv != null ){
                instance.ws_flv= this.ws_flv.clone();
            }
            if (this.hls != null ) {
                instance.hls= this.hls.clone();
            }
            if (this.ws_hls != null ) {
                instance.ws_hls= this.ws_hls.clone();
            }
            if (this.ts != null ) {
                instance.ts= this.ts.clone();
            }
            if (this.ws_ts != null ) {
                instance.ws_ts= this.ws_ts.clone();
            }
            if (this.fmp4 != null ) {
                instance.fmp4= this.fmp4.clone();
            }
            if (this.ws_fmp4 != null ) {
                instance.ws_fmp4= this.ws_fmp4.clone();
            }
            if (this.rtc != null ) {
                instance.rtc= this.rtc.clone();
            }
            if (this.https_flv != null) {
                instance.https_flv= this.https_flv.clone();
            }
            if (this.wss_flv != null) {
                instance.wss_flv= this.wss_flv.clone();
            }
            if (this.https_hls != null) {
                instance.https_hls= this.https_hls.clone();
            }
            if (this.wss_hls != null) {
                instance.wss_hls= this.wss_hls.clone();
            }
            if (this.wss_ts != null) {
                instance.wss_ts= this.wss_ts.clone();
            }
            if (this.https_fmp4 != null) {
                instance.https_fmp4= this.https_fmp4.clone();
            }
            if (this.wss_fmp4 != null) {
                instance.wss_fmp4= this.wss_fmp4.clone();
            }
            if (this.rtcs != null) {
                instance.rtcs= this.rtcs.clone();
            }
            if (this.rtsp != null) {
                instance.rtsp= this.rtsp.clone();
            }
            if (this.rtsps != null) {
                instance.rtsps= this.rtsps.clone();
            }
            if (this.rtmp != null) {
                instance.rtmp= this.rtmp.clone();
            }
            if (this.rtmps != null) {
                instance.rtmps= this.rtmps.clone();
            }
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return instance;
    }

}
