package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;


@Configuration("mediaConfig")
public class MediaConfig{

    // 修改必须配置，不再支持自动获取
    @Value("${media.id}")
    private String id;

    @Value("${media.ip}")
    private String ip;

    @Value("${media.hook-ip:${sip.ip}}")
    private String hookIp;

    @Value("${sip.ip}")
    private String sipIp;

    @Value("${sip.domain}")
    private String sipDomain;

    @Value("${media.sdp-ip:${media.ip}}")
    private String sdpIp;

    @Value("${media.stream-ip:${media.ip}}")
    private String streamIp;

    @Value("${media.http-port}")
    private Integer httpPort;

    @Value("${media.http-ssl-port:0}")
    private Integer httpSSlPort = 0;

    @Value("${media.rtmp-port:0}")
    private Integer rtmpPort = 0;

    @Value("${media.rtmp-ssl-port:0}")
    private Integer rtmpSSlPort = 0;

    @Value("${media.rtp-proxy-port:0}")
    private Integer rtpProxyPort = 0;

    @Value("${media.rtsp-port:0}")
    private Integer rtspPort = 0;

    @Value("${media.rtsp-ssl-port:0}")
    private Integer rtspSSLPort = 0;

    @Value("${media.auto-config:true}")
    private boolean autoConfig = true;

    @Value("${media.secret}")
    private String secret;

    @Value("${media.stream-none-reader-delay-ms:10000}")
    private int streamNoneReaderDelayMS = 10000;

    @Value("${media.rtp.enable}")
    private boolean rtpEnable;

    @Value("${media.rtp.port-range}")
    private String rtpPortRange;


    @Value("${media.rtp.send-port-range}")
    private String sendRtpPortRange;

    @Value("${media.record-assist-port:0}")
    private Integer recordAssistPort = 0;

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getHookIp() {
        if (StringUtils.isEmpty(hookIp)){
            return sipIp;
        }else {
            return hookIp;
        }

    }

    public String getSipIp() {
        if (sipIp == null) {
            return this.ip;
        }else {
            return sipIp;
        }
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getHttpSSlPort() {
        return httpSSlPort;
    }

    public int getRtmpPort() {
        return rtmpPort;
    }
    
    public int getRtmpSSlPort() {
        return rtmpSSlPort;
    }

    public int getRtpProxyPort() {
        if (rtpProxyPort == null) {
            return 0;
        }else {
            return rtpProxyPort;
        }

    }

    public int getRtspPort() {
        return rtspPort;
    }

    public int getRtspSSLPort() {
        return rtspSSLPort;
    }

    public boolean isAutoConfig() {
        return autoConfig;
    }

    public String getSecret() {
        return secret;
    }

    public int getStreamNoneReaderDelayMS() {
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

    public String getSdpIp() {
        if (StringUtils.isEmpty(sdpIp)){
            return ip;
        }else {
            if (isValidIPAddress(sdpIp)) {
                return sdpIp;
            }else {
                // 按照域名解析
                String hostAddress = null;
                try {
                    hostAddress = InetAddress.getByName(sdpIp).getHostAddress();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                return hostAddress;
            }
        }
    }

    public String getStreamIp() {
        if (StringUtils.isEmpty(streamIp)){
            return ip;
        }else {
            return streamIp;
        }
    }

    public String getSipDomain() {
        return sipDomain;
    }

    public String getSendRtpPortRange() {
        return sendRtpPortRange;
    }

    public MediaServerItem getMediaSerItem(){
        MediaServerItem mediaServerItem = new MediaServerItem();
        mediaServerItem.setId(id);
        mediaServerItem.setIp(ip);
        mediaServerItem.setDefaultServer(true);
        mediaServerItem.setHookIp(getHookIp());
        mediaServerItem.setSdpIp(getSdpIp());
        mediaServerItem.setStreamIp(getStreamIp());
        mediaServerItem.setHttpPort(httpPort);
        mediaServerItem.setHttpSSlPort(httpSSlPort);
        mediaServerItem.setRtmpPort(rtmpPort);
        mediaServerItem.setRtmpSSlPort(rtmpSSlPort);
        mediaServerItem.setRtpProxyPort(getRtpProxyPort());
        mediaServerItem.setRtspPort(rtspPort);
        mediaServerItem.setRtspSSLPort(rtspSSLPort);
        mediaServerItem.setAutoConfig(autoConfig);
        mediaServerItem.setSecret(secret);
        mediaServerItem.setStreamNoneReaderDelayMS(streamNoneReaderDelayMS);
        mediaServerItem.setRtpEnable(rtpEnable);
        mediaServerItem.setRtpPortRange(rtpPortRange);
        mediaServerItem.setSendRtpPortRange(sendRtpPortRange);
        mediaServerItem.setRecordAssistPort(recordAssistPort);
        mediaServerItem.setHookAliveInterval(120);

        mediaServerItem.setCreateTime(DateUtil.getNow());
        mediaServerItem.setUpdateTime(DateUtil.getNow());

        return mediaServerItem;
    }

    private boolean isValidIPAddress(String ipAddress) {
        if ((ipAddress != null) && (!ipAddress.isEmpty())) {
            return Pattern.matches("^([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$", ipAddress);
        }
        return false;
    }

}
