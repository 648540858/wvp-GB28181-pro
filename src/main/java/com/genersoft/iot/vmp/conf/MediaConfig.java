package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;


/**
 * 对配置文件进行校验
 */
@Configuration("mediaConfig")
public class MediaConfig {
    @Value("${media.ip}")
    private String mediaIp;
    private String[] mediaIpArr;

    @Value("${media.hookIp}")
    private String mediaHookIp;

    @Value("${media.port}")
    private Integer mediaPort;

    @Value("${media.autoConfig}")
    private Boolean autoConfig;

    @Value("${media.secret}")
    private String mediaSecret;

    @Value("${media.streamNoneReaderDelayMS}")
    private String streamNoneReaderDelayMS;

    @Value("${media.autoApplyPlay}")
    private Boolean autoApplyPlay;

    @Value("${media.seniorSdp}")
    private Boolean seniorSdp;

    @Value("${media.rtp.enable}")
    private Boolean rtpEnable;

    @Value("${media.rtp.udpPortRange}")
    private String udpPortRange;

    /**
     * 每一台ZLM都有一套独立的SSRC列表
     * 在ApplicationCheckRunner里对mediaServerSsrcMap进行初始化
     */
    private HashMap<String, SsrcConfig> mediaServerSsrcMap;

    public String getMediaIp() {
        return mediaIp;
    }

    public void setMediaIp(String mediaIp) {
        this.mediaIp = mediaIp;
    }

    public String[] getMediaIpArr() {
        return mediaIpArr;
    }

    public void setMediaIpArr(String[] mediaIpArr) {
        this.mediaIpArr = mediaIpArr;
    }

    public String getMediaHookIp() {
        return mediaHookIp;
    }

    public void setMediaHookIp(String mediaHookIp) {
        this.mediaHookIp = mediaHookIp;
    }

    public Integer getMediaPort() {
        return mediaPort;
    }

    public void setMediaPort(Integer mediaPort) {
        this.mediaPort = mediaPort;
    }

    public Boolean getAutoConfig() {
        return autoConfig;
    }

    public void setAutoConfig(Boolean autoConfig) {
        this.autoConfig = autoConfig;
    }

    public String getMediaSecret() {
        return mediaSecret;
    }

    public void setMediaSecret(String mediaSecret) {
        this.mediaSecret = mediaSecret;
    }

    public String getStreamNoneReaderDelayMS() {
        return streamNoneReaderDelayMS;
    }

    public void setStreamNoneReaderDelayMS(String streamNoneReaderDelayMS) {
        this.streamNoneReaderDelayMS = streamNoneReaderDelayMS;
    }

    public Boolean getAutoApplyPlay() {
        return autoApplyPlay;
    }

    public void setAutoApplyPlay(Boolean autoApplyPlay) {
        this.autoApplyPlay = autoApplyPlay;
    }

    public Boolean getSeniorSdp() {
        return seniorSdp;
    }

    public void setSeniorSdp(Boolean seniorSdp) {
        this.seniorSdp = seniorSdp;
    }

    public Boolean getRtpEnable() {
        return rtpEnable;
    }

    public void setRtpEnable(Boolean rtpEnable) {
        this.rtpEnable = rtpEnable;
    }

    public String getUdpPortRange() {
        return udpPortRange;
    }

    public void setUdpPortRange(String udpPortRange) {
        this.udpPortRange = udpPortRange;
    }

    public HashMap<String, SsrcConfig> getMediaServerSsrcMap() {
        return mediaServerSsrcMap;
    }

    public void setMediaServerSsrcMap(HashMap<String, SsrcConfig> mediaServerSsrcMap) {
        this.mediaServerSsrcMap = mediaServerSsrcMap;
    }
}
