package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Slf4j
@Configuration("mediaConfig")
@Order(0)
@Data
public class MediaConfig{

    // 修改必须配置，不再支持自动获取
    @Value("${media.id}")
    private String id;

    @Value("${media.ip}")
    private String ip;

    @Value("${media.wan_ip:}")
    private String wanIp;

    @Value("${media.hook-ip:127.0.0.1}")
    private String hookIp;

    @Value("${sip.domain}")
    private String sipDomain;

    @Value("${media.sdp-ip:${media.wan_ip:}}")
    private String sdpIp;

    @Value("${media.stream-ip:${media.wan_ip:}}")
    private String streamIp;

    @Value("${media.http-port:0}")
    private Integer httpPort;

    @Value("${media.flv-port:0}")
    private Integer flvPort = 0;

    @Value("${media.ws-flv-port:0}")
    private Integer wsFlvPort = 0;

    @Value("${media.http-ssl-port:0}")
    private Integer httpSSlPort = 0;

    @Value("${media.flv-ssl-port:0}")
    private Integer flvSSlPort = 0;

    @Value("${media.ws-flv-ssl-port:0}")
    private Integer wsFlvSSlPort = 0;

    @Value("${media.rtmp-port:0}")
    private Integer rtmpPort = 0;

    @Value("${media.rtmp-ssl-port:0}")
    private Integer rtmpSSlPort = 0;

    @Value("${media.rtp-proxy-port:0}")
    private Integer rtpProxyPort = 0;

    @Value("${media.jtt-proxy-port:0}")
    private Integer jttProxyPort = 0;

    @Value("${media.rtsp-port:0}")
    private Integer rtspPort = 0;

    @Value("${media.rtsp-ssl-port:0}")
    private Integer rtspSSLPort = 0;

    @Value("${media.auto-config:true}")
    private boolean autoConfig = true;

    @Value("${media.secret}")
    private String secret;

    @Value("${media.rtp.enable}")
    private boolean rtpEnable;

    @Value("${media.rtp.port-range}")
    private String rtpPortRange;

    @Value("${media.rtp.send-port-range}")
    private String rtpSendPortRange;

    @Value("${media.record-assist-port:0}")
    private Integer recordAssistPort = 0;

    @Value("${media.record-day:7}")
    private Integer recordDay;

    @Value("${media.record-path:}")
    private String recordPath;

    @Value("${media.type:zlm}")
    private String type;



    public int getRtpProxyPort() {
        if (rtpProxyPort == null) {
            return 0;
        }else {
            return rtpProxyPort;
        }

    }

    public Integer getJttProxyPort() {
        if (jttProxyPort == null) {
            return 0;
        }else {
            return jttProxyPort;
        }
    }

    public String getSdpIp() {
        if (ObjectUtils.isEmpty(sdpIp)){
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
                    log.error("[获取SDP IP]: 域名解析失败");
                }
                return hostAddress;
            }
        }
    }

    public String getStreamIp() {
        if (ObjectUtils.isEmpty(streamIp)){
            return ip;
        }else {
            return streamIp;
        }
    }

    public MediaServer getMediaSerItem(){
        MediaServer mediaServer = new MediaServer();
        mediaServer.setId(id);
        mediaServer.setIp(ip);
        mediaServer.setDefaultServer(true);
        mediaServer.setHookIp(getHookIp());
        mediaServer.setSdpIp(getSdpIp());
        mediaServer.setStreamIp(getStreamIp());
        mediaServer.setHttpPort(httpPort);
        if (flvPort == 0) {
            mediaServer.setFlvPort(httpPort);
        }else {
            mediaServer.setFlvPort(flvPort);
        }
        if (wsFlvPort == 0) {
            mediaServer.setWsFlvPort(httpPort);
        }else {
            mediaServer.setWsFlvPort(wsFlvPort);
        }
        if (flvSSlPort == 0) {
            mediaServer.setFlvSSLPort(httpSSlPort);
        }else {
            mediaServer.setFlvSSLPort(flvSSlPort);
        }
        if (wsFlvSSlPort == 0) {
            mediaServer.setWsFlvSSLPort(httpSSlPort);
        }else {
            mediaServer.setWsFlvSSLPort(wsFlvSSlPort);
        }

        mediaServer.setHttpSSlPort(httpSSlPort);
        mediaServer.setRtmpPort(rtmpPort);
        mediaServer.setRtmpSSlPort(rtmpSSlPort);
        mediaServer.setRtpProxyPort(getRtpProxyPort());
        mediaServer.setJttProxyPort(getJttProxyPort());
        mediaServer.setRtspPort(rtspPort);
        mediaServer.setRtspSSLPort(rtspSSLPort);
        mediaServer.setAutoConfig(autoConfig);
        mediaServer.setSecret(secret);
        mediaServer.setRtpEnable(rtpEnable);
        mediaServer.setRtpPortRange(rtpPortRange);
        mediaServer.setSendRtpPortRange(rtpSendPortRange);
        mediaServer.setRecordAssistPort(recordAssistPort);
        mediaServer.setHookAliveInterval(10f);
        mediaServer.setRecordDay(recordDay);
        mediaServer.setStatus(false);
        mediaServer.setType(type);
        if (recordPath != null) {
            mediaServer.setRecordPath(recordPath);
        }
        mediaServer.setCreateTime(DateUtil.getNow());
        mediaServer.setUpdateTime(DateUtil.getNow());

        return mediaServer;
    }

    private boolean isValidIPAddress(String ipAddress) {
        if ((ipAddress != null) && (!ipAddress.isEmpty())) {
            return Pattern.matches("^([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$", ipAddress);
        }
        return false;
    }
}
