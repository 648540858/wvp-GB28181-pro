package com.genersoft.iot.vmp.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;


/**
 * 对配置文件进行校验
 */
@Configuration("mediaConfig")
@Data
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

}
