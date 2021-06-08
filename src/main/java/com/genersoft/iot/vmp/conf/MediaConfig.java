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

    @Value("${media.sdpIp:${media.ip}}")
    private String sdpIp;

    @Value("${media.streamIp:${media.ip}}")
    private String streamIp;

    @Value("${media.httpPort}")
    private String httpPort;

    @Value("${media.httpSSlPort:}")
    private String httpSSlPort;

    @Value("${media.rtmpPort:}")
    private String rtmpPort;

    @Value("${media.rtmpSSlPort:}")
    private String rtmpSSlPort;

    @Value("${media.rtpProxyPort:}")
    private String rtpProxyPort;

    @Value("${media.rtspPort:}")
    private String rtspPort;

    @Value("${media.rtspSSLPort:}")
    private String rtspSSLPort;

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
    private int recordAssistPort;

    public String getIp() {
        return ip;
    }

    public String getHookIp() {
        return hookIp;
    }

    public String getSdpIp() {
        return sdpIp;
    }

    public String getStreamIp() {
        return streamIp;
    }

    public String getHttpPort() {
        return httpPort;
    }

    public String getHttpSSlPort() {
        return httpSSlPort;
    }

    public String getRtmpPort() {
        return rtmpPort;
    }

    public String getRtmpSSlPort() {
        return rtmpSSlPort;
    }

    public String getRtpProxyPort() {
        return rtpProxyPort;
    }

    public String getRtspPort() {
        return rtspPort;
    }

    public String getRtspSSLPort() {
        return rtspSSLPort;
    }

    public boolean isAutoConfig() {
        return autoConfig;
    }

    public String getSecret() {
        return secret;
    }

    public String getStreamNoneReaderDelayMS() {
        return streamNoneReaderDelayMS;
    }

    public boolean isRtpEnable() {
        return rtpEnable;
    }

    public String getRtpPortRange() {
        return rtpPortRange;
    }

    public int getRecordAssistPort() {
        return recordAssistPort;
    }
}
