package com.genersoft.iot.vmp.media.zlm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author lin
 */
@Schema(description = "拉流代理的信息")
public class StreamProxy {
    @Schema(description = "ID")
    private int id;
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
    @Schema(description = "目标地址")
    private String dstUrl;
    @Schema(description = "超时时间")
    private int timeoutMs;
    @Schema(description = "ffmpeg模板KEY")
    private String ffmpegCmdKey;
    @Schema(description = "rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播")
    private String rtpType;
    @Schema(description = "代理失败的原因")
    private String proxyError;
    @Schema(description = "是否启用")
    private boolean enable;
    @Schema(description = "是否启用音频")
    private boolean enableAudio;
    @Schema(description = "是否录制")
    private boolean enableMp4;
    @Schema(description = "是否 无人观看时删除")
    private boolean enableRemoveNoneReader;

    @Schema(description = "是否 无人观看时自动停用")
    private boolean enableDisableNoneReader;

    @Schema(description = "拉流代理时zlm返回的key，用于停止拉流代理")
    private String streamKey;

    @Schema(description = "国标ID")
    private String gbId;

    @Schema(description = "名称")
    private String name;
    @Schema(description = "经度")
    private double longitude;
    @Schema(description = "纬度")
    private double latitude;
    @Schema(description = "状态")
    private boolean status;
    @Schema(description = "创建时间")
    private String createTime;
    @Schema(description = "更新时间")
    private String updateTime;

    /**
     * 国标通用信息ID
     */
    @Schema(description = "国标通用信息ID")
    private int commonGbChannelId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public String getDstUrl() {
        return dstUrl;
    }

    public void setDstUrl(String dstUrl) {
        this.dstUrl = dstUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getFfmpegCmdKey() {
        return ffmpegCmdKey;
    }

    public void setFfmpegCmdKey(String ffmpegCmdKey) {
        this.ffmpegCmdKey = ffmpegCmdKey;
    }

    public String getRtpType() {
        return rtpType;
    }

    public void setRtpType(String rtpType) {
        this.rtpType = rtpType;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isEnableAudio() {
        return enableAudio;
    }

    public void setEnableAudio(boolean enableAudio) {
        this.enableAudio = enableAudio;
    }

    public boolean isEnableMp4() {
        return enableMp4;
    }

    public void setEnableMp4(boolean enableMp4) {
        this.enableMp4 = enableMp4;
    }

    public boolean isEnableRemoveNoneReader() {
        return enableRemoveNoneReader;
    }

    public void setEnableRemoveNoneReader(boolean enableRemoveNoneReader) {
        this.enableRemoveNoneReader = enableRemoveNoneReader;
    }

    public boolean isEnableDisableNoneReader() {
        return enableDisableNoneReader;
    }

    public void setEnableDisableNoneReader(boolean enableDisableNoneReader) {
        this.enableDisableNoneReader = enableDisableNoneReader;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public int getCommonGbChannelId() {
        return commonGbChannelId;
    }

    public void setCommonGbChannelId(int commonGbChannelId) {
        this.commonGbChannelId = commonGbChannelId;
    }

    public String getProxyError() {
        return proxyError;
    }

    public void setProxyError(String proxyError) {
        this.proxyError = proxyError;
    }
}
