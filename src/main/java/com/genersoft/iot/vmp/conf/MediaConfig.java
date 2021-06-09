package com.genersoft.iot.vmp.conf;

import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration("mediaConfig")
public class MediaConfig {

    @Value("${media.ip}")
    private String ip;

    @Value("${media.hookIp:${sip.ip}}")
    private String hookIp;

    @Value("${sip.ip}")
    private String sipIp;

    @Value("${media.sdpIp:${media.ip}}")
    private String sdpIp;

    @Value("${media.streamIp:${media.ip}}")
    private String streamIp;

    @Value("${media.httpPort}")
    private Integer httpPort;

    @Value("${media.httpSSlPort:}")
    private Integer httpSSlPort;

    @Value("${media.rtmpPort:}")
    private Integer rtmpPort;

    @Value("${media.rtmpSSlPort:}")
    private Integer rtmpSSlPort;

    @Value("${media.rtpProxyPort:}")
    private Integer rtpProxyPort;

    @Value("${media.rtspPort:}")
    private Integer rtspPort;

    @Value("${media.rtspSSLPort:}")
    private Integer rtspSSLPort;

    @Value("${media.autoConfig:true}")
    private boolean autoConfig;

    @Value("${media.secret}")
    private String secret;

    @Value("${media.streamNoneReaderDelayMS:18000}")
    private String streamNoneReaderDelayMS;

    @Value("${media.rtp.enable}")
    private boolean rtpEnable;

    @Value("${media.rtp.portRange}")
    private String rtpPortRange;

    @Value("${media.recordAssistPort}")
    private Integer recordAssistPort;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHookIp() {
        if (StringUtils.isEmpty(hookIp)){
            return sipIp;
        }else {
            return hookIp;
        }

    }

    public void setHookIp(String hookIp) {
        this.hookIp = hookIp;
    }

    public String getSdpIp() {
        if (StringUtils.isEmpty(sdpIp)){
            return ip;
        }else {
            return sdpIp;
        }
    }

    public void setSdpIp(String sdpIp) {
        this.sdpIp = sdpIp;
    }

    public String getStreamIp() {
        if (StringUtils.isEmpty(streamIp)){
            return ip;
        }else {
            return streamIp;
        }
    }

    public void setStreamIp(String streamIp) {
        this.streamIp = streamIp;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpSSlPort() {
        return httpSSlPort;
    }

    public void setHttpSSlPort(Integer httpSSlPort) {
        this.httpSSlPort = httpSSlPort;
    }

    public Integer getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(Integer rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public Integer getRtmpSSlPort() {
        return rtmpSSlPort;
    }

    public void setRtmpSSlPort(Integer rtmpSSlPort) {
        this.rtmpSSlPort = rtmpSSlPort;
    }

    public Integer getRtpProxyPort() {
        return rtpProxyPort;
    }

    public void setRtpProxyPort(Integer rtpProxyPort) {
        this.rtpProxyPort = rtpProxyPort;
    }

    public Integer getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(Integer rtspPort) {
        this.rtspPort = rtspPort;
    }

    public Integer getRtspSSLPort() {
        return rtspSSLPort;
    }

    public void setRtspSSLPort(Integer rtspSSLPort) {
        this.rtspSSLPort = rtspSSLPort;
    }

    public boolean isAutoConfig() {
        return autoConfig;
    }

    public void setAutoConfig(boolean autoConfig) {
        this.autoConfig = autoConfig;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getStreamNoneReaderDelayMS() {
        return streamNoneReaderDelayMS;
    }

    public void setStreamNoneReaderDelayMS(String streamNoneReaderDelayMS) {
        this.streamNoneReaderDelayMS = streamNoneReaderDelayMS;
    }

    public boolean isRtpEnable() {
        return rtpEnable;
    }

    public void setRtpEnable(boolean rtpEnable) {
        this.rtpEnable = rtpEnable;
    }

    public String getRtpPortRange() {
        return rtpPortRange;
    }

    public void setRtpPortRange(String rtpPortRange) {
        this.rtpPortRange = rtpPortRange;
    }

    public Integer getRecordAssistPort() {
        return recordAssistPort;
    }

    public void setRecordAssistPort(Integer recordAssistPort) {
        this.recordAssistPort = recordAssistPort;
    }
}
