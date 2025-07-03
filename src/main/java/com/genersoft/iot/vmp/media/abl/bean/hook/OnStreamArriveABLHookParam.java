package com.genersoft.iot.vmp.media.abl.bean.hook;

import com.genersoft.iot.vmp.media.abl.bean.AblUrls;

/**
 * 流到来的事件
 */
public class OnStreamArriveABLHookParam extends ABLHookParam{



    /**
     * 推流鉴权Id
     */
    private String callId;

    /**
     * 状态
     */
    private Boolean status;


    /**
     *
     */
    private Boolean enableHls;


    /**
     *
     */
    private Boolean transcodingStatus;


    /**
     *
     */
    private String sourceURL;


    /**
     *
     */
    private Integer readerCount;


    /**
     *
     */
    private Integer noneReaderDuration;


    /**
     *
     */
    private String videoCodec;


    /**
     *
     */
    private Integer videoFrameSpeed;


    /**
     *
     */
    private Integer width;


    /**
     *
     */
    private Integer height;


    /**
     *
     */
    private Integer videoBitrate;


    /**
     *
     */
    private String audioCodec;


    /**
     *
     */
    private Integer audioChannels;


    /**
     *
     */
    private Integer audioSampleRate;


    /**
     *
     */
    private Integer audioBitrate;


    private AblUrls url;


    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getEnableHls() {
        return enableHls;
    }

    public void setEnableHls(Boolean enableHls) {
        this.enableHls = enableHls;
    }

    public Boolean getTranscodingStatus() {
        return transcodingStatus;
    }

    public void setTranscodingStatus(Boolean transcodingStatus) {
        this.transcodingStatus = transcodingStatus;
    }

    public String getSourceURL() {
        return sourceURL;
    }

    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    public Integer getReaderCount() {
        return readerCount;
    }

    public void setReaderCount(Integer readerCount) {
        this.readerCount = readerCount;
    }

    public Integer getNoneReaderDuration() {
        return noneReaderDuration;
    }

    public void setNoneReaderDuration(Integer noneReaderDuration) {
        this.noneReaderDuration = noneReaderDuration;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public Integer getVideoFrameSpeed() {
        return videoFrameSpeed;
    }

    public void setVideoFrameSpeed(Integer videoFrameSpeed) {
        this.videoFrameSpeed = videoFrameSpeed;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(Integer videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public Integer getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(Integer audioChannels) {
        this.audioChannels = audioChannels;
    }

    public Integer getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(Integer audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public Integer getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(Integer audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public AblUrls getUrl() {
        return url;
    }

    public void setUrl(AblUrls url) {
        this.url = url;
    }
}
