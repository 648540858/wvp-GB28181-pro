package com.genersoft.iot.vmp.media.zlm.dto;

public class StreamProxyDto {
    private String type;
    private String app;
    private String stream;
    private String url;
    private String src_url;
    private String dst_url;
    private int timeout_ms;
    private String ffmpeg_cmd_key;
    private String rtp_type;
    private boolean enable;
    private boolean enable_hls;
    private boolean enable_mp4;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSrc_url() {
        return src_url;
    }

    public void setSrc_url(String src_url) {
        this.src_url = src_url;
    }

    public String getDst_url() {
        return dst_url;
    }

    public void setDst_url(String dst_url) {
        this.dst_url = dst_url;
    }

    public int getTimeout_ms() {
        return timeout_ms;
    }

    public void setTimeout_ms(int timeout_ms) {
        this.timeout_ms = timeout_ms;
    }

    public String getFfmpeg_cmd_key() {
        return ffmpeg_cmd_key;
    }

    public void setFfmpeg_cmd_key(String ffmpeg_cmd_key) {
        this.ffmpeg_cmd_key = ffmpeg_cmd_key;
    }

    public String getRtp_type() {
        return rtp_type;
    }

    public void setRtp_type(String rtp_type) {
        this.rtp_type = rtp_type;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isEnable_hls() {
        return enable_hls;
    }

    public void setEnable_hls(boolean enable_hls) {
        this.enable_hls = enable_hls;
    }

    public boolean isEnable_mp4() {
        return enable_mp4;
    }

    public void setEnable_mp4(boolean enable_mp4) {
        this.enable_mp4 = enable_mp4;
    }
}
