package com.genersoft.iot.vmp.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Objects;

@Schema(description = "流信息")
public class StreamInfo implements Serializable, Cloneable{

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

    public void setFlv(StreamURL flv) {
        this.flv = flv;
    }

    public void setHttps_flv(StreamURL https_flv) {
        this.https_flv = https_flv;
    }

    public void setWs_flv(StreamURL ws_flv) {
        this.ws_flv = ws_flv;
    }

    public void setWss_flv(StreamURL wss_flv) {
        this.wss_flv = wss_flv;
    }

    public void setFmp4(StreamURL fmp4) {
        this.fmp4 = fmp4;
    }

    public void setHttps_fmp4(StreamURL https_fmp4) {
        this.https_fmp4 = https_fmp4;
    }

    public void setWs_fmp4(StreamURL ws_fmp4) {
        this.ws_fmp4 = ws_fmp4;
    }

    public void setWss_fmp4(StreamURL wss_fmp4) {
        this.wss_fmp4 = wss_fmp4;
    }

    public void setHls(StreamURL hls) {
        this.hls = hls;
    }

    public void setHttps_hls(StreamURL https_hls) {
        this.https_hls = https_hls;
    }

    public void setWs_hls(StreamURL ws_hls) {
        this.ws_hls = ws_hls;
    }

    public void setWss_hls(StreamURL wss_hls) {
        this.wss_hls = wss_hls;
    }

    public void setTs(StreamURL ts) {
        this.ts = ts;
    }

    public void setHttps_ts(StreamURL https_ts) {
        this.https_ts = https_ts;
    }

    public void setWs_ts(StreamURL ws_ts) {
        this.ws_ts = ws_ts;
    }

    public void setWss_ts(StreamURL wss_ts) {
        this.wss_ts = wss_ts;
    }

    public void setRtmp(StreamURL rtmp) {
        this.rtmp = rtmp;
    }

    public void setRtmps(StreamURL rtmps) {
        this.rtmps = rtmps;
    }

    public void setRtsp(StreamURL rtsp) {
        this.rtsp = rtsp;
    }

    public void setRtsps(StreamURL rtsps) {
        this.rtsps = rtsps;
    }

    public void setRtc(StreamURL rtc) {
        this.rtc = rtc;
    }

    public void setRtcs(StreamURL rtcs) {
        this.rtcs = rtcs;
    }

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

    public void setFlv(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.flv%s", app, stream, callIdParam);
        if (port > 0) {
            this.flv = new StreamURL("http", host, port, file);
        }
        this.ws_flv = new StreamURL("ws", host, port, file);
        if (sslPort > 0) {
            this.https_flv = new StreamURL("https", host, sslPort, file);
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

    public void setRtc(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        if (callIdParam != null) {
            callIdParam = Objects.equals(callIdParam, "") ? callIdParam : callIdParam.replace("?", "&");
        }
        String file = String.format("index/api/webrtc?app=%s&stream=%s&type=play%s", app, stream, callIdParam);
        if (port > 0) {
            this.rtc = new StreamURL("http", host, port, file);
        }
        if (sslPort > 0) {
            this.rtcs = new StreamURL("https", host, sslPort, file);
        }
    }

    public void channgeStreamIp(String localAddr) {
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public StreamURL getFlv() {
        return flv;
    }

    public StreamURL getHttps_flv() {
        return https_flv;
    }

    public StreamURL getWs_flv() {
        return ws_flv;
    }


    public StreamURL getWss_flv() {
        return wss_flv;
    }

    public StreamURL getFmp4() {
        return fmp4;
    }



    public StreamURL getHttps_fmp4() {
        return https_fmp4;
    }

    public StreamURL getWs_fmp4() {
        return ws_fmp4;
    }

    public StreamURL getWss_fmp4() {
        return wss_fmp4;
    }

    public StreamURL getHls() {
        return hls;
    }


    public StreamURL getHttps_hls() {
        return https_hls;
    }

    public StreamURL getWs_hls() {
        return ws_hls;
    }

    public StreamURL getWss_hls() {
        return wss_hls;
    }

    public StreamURL getTs() {
        return ts;
    }


    public StreamURL getHttps_ts() {
        return https_ts;
    }


    public StreamURL getWs_ts() {
        return ws_ts;
    }


    public StreamURL getWss_ts() {
        return wss_ts;
    }


    public StreamURL getRtmp() {
        return rtmp;
    }

    public StreamURL getRtmps() {
        return rtmps;
    }

    public StreamURL getRtsp() {
        return rtsp;
    }

    public StreamURL getRtsps() {
        return rtsps;
    }

    public StreamURL getRtc() {
        return rtc;
    }

    public StreamURL getRtcs() {
        return rtcs;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public Object getTracks() {
        return tracks;
    }

    public void setTracks(Object tracks) {
        this.tracks = tracks;
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

    public boolean isPause() {
        return pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    public void setTransactionInfo(TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

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


    /*=========================设备主子码流逻辑START====================*/
    @Schema(description = "是否为子码流(true-是，false-主码流)")
    private boolean subStream;

    public boolean isSubStream() {
        return subStream;
    }

    public void setSubStream(boolean subStream) {
        this.subStream = subStream;
    }


}
