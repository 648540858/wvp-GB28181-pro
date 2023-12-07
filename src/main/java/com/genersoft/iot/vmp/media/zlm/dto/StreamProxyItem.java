package com.genersoft.iot.vmp.media.zlm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author lin
 */
@Schema(description = "拉流代理的信息")
public class StreamProxyItem {

    @Schema(description = "类型")
    private String type;
    @Schema(description = "应用名")
    private String app;
    @Schema(description = "流ID")
    private String stream;
    @Schema(description = "流媒体服务ID")
    private String mediaServerId;
    @Schema(description = "拉流地址")
    private String url;
    @Schema(description = "拉流地址")
    private String srcUrl;
    @Schema(description = "目标地址")
    private String dstUrl;
    @Schema(description = "超时时间")
    private int timeoutMs;
    @Schema(description = "ffmpeg模板KEY")
    private String ffmpegCmdKey;
    @Schema(description = "rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播")
    private String rtpType;
    @Schema(description = "是否启用")
    private boolean enable;
    @Schema(description = "是否启用音频")
    private boolean enableAudio;
    @Schema(description = "是否启用MP4")
    private boolean enableMp4;
    @Schema(description = "是否 无人观看时删除")
    private boolean enableRemoveNoneReader;

    @Schema(description = "是否 无人观看时自动停用")
    private boolean enableDisableNoneReader;

    @Schema(description = "拉流代理时zlm返回的key，用于停止拉流代理")
    private String streamKey;

    /**
     * 国标通用信息ID
     */
    @Schema(description = "国标通用信息ID")
    private int commonGbChannelId;

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

    
    public String getMediaServerId() {
        return mediaServerId;
    }

    
    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSrcUrl() {
        return srcUrl;
    }

    public void setSrcUrl(String src_url) {
        this.srcUrl = src_url;
    }

    public String getDstUrl() {
        return dstUrl;
    }

    public void setDstUrl(String dst_url) {
        this.dstUrl = dst_url;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeout_ms) {
        this.timeoutMs = timeout_ms;
    }

    public String getFfmpegCmdKey() {
        return ffmpegCmdKey;
    }

    public void setFfmpegCmdKey(String ffmpeg_cmd_key) {
        this.ffmpegCmdKey = ffmpeg_cmd_key;
    }

    public String getRtpType() {
        return rtpType;
    }

    public void setRtpType(String rtp_type) {
        this.rtpType = rtp_type;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isEnableMp4() {
        return enableMp4;
    }

    public void setEnableMp4(boolean enable_mp4) {
        this.enableMp4 = enable_mp4;
    }

    public boolean isEnableRemoveNoneReader() {
        return enableRemoveNoneReader;
    }

    public void setEnableRemoveNoneReader(boolean enable_remove_none_reader) {
        this.enableRemoveNoneReader = enable_remove_none_reader;
    }

    public boolean isEnableDisableNoneReader() {
        return enableDisableNoneReader;
    }

    public void setEnableDisableNoneReader(boolean enable_disable_none_reader) {
        this.enableDisableNoneReader = enable_disable_none_reader;
    }

    public boolean isEnableAudio() {
        return enableAudio;
    }

    public void setEnableAudio(boolean enable_audio) {
        this.enableAudio = enable_audio;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public int getCommonGbChannelId() {
        return commonGbChannelId;
    }

    public void setCommonGbChannelId(int commonGbChannelId) {
        this.commonGbChannelId = commonGbChannelId;
    }
}
