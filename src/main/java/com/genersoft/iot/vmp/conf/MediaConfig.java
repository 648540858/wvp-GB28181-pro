package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.media.zlm.dto.IMediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration("mediaConfig")
public class MediaConfig implements IMediaServerItem {

    @Value("${media.id:}")
    private String id;

    @Value("${media.ip}")
    private String ip;

    @Value("${media.hook-ip:${sip.ip}}")
    private String hookIp;

    @Value("${sip.ip}")
    private String sipIp;

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

    @Value("${media.stream-none-reader-delay-ms:18000}")
    private String streamNoneReaderDelayMS = "18000";

    @Value("${media.rtp.enable}")
    private boolean rtpEnable;

    @Value("${media.rtp.port-range}")
    private String rtpPortRange;

    @Value("${media.record-assist-port:0}")
    private Integer recordAssistPort = 0;

    private String updateTime;

    private String createTime;

    private boolean docker = false;

    private int count;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
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

    public String getSipIp() {
        return sipIp;
    }

    public void setSipIp(String sipIp) {
        this.sipIp = sipIp;
    }

    public void setSdpIp(String sdpIp) {
        this.sdpIp = sdpIp;
    }

    public void setStreamIp(String streamIp) {
        this.streamIp = streamIp;
    }

    public int getHttpPort() {
        return httpPort;
    }

    @Override
    public void setHttpPort(int httpPort) {

    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpSSlPort() {
        return httpSSlPort;
    }

    @Override
    public void setHttpSSlPort(int httpSSlPort) {

    }

    public void setHttpSSlPort(Integer httpSSlPort) {
        this.httpSSlPort = httpSSlPort;
    }

    public int getRtmpPort() {
        return rtmpPort;
    }

    @Override
    public void setRtmpPort(int rtmpPort) {

    }

    public void setRtmpPort(Integer rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public int getRtmpSSlPort() {
        return rtmpSSlPort;
    }

    @Override
    public void setRtmpSSlPort(int rtmpSSlPort) {

    }

    public void setRtmpSSlPort(Integer rtmpSSlPort) {
        this.rtmpSSlPort = rtmpSSlPort;
    }

    public int getRtpProxyPort() {
        if (rtpProxyPort == null) {
            return 0;
        }else {
            return rtpProxyPort;
        }

    }

    @Override
    public void setRtpProxyPort(int rtpProxyPort) {

    }

    public void setRtpProxyPort(Integer rtpProxyPort) {
        this.rtpProxyPort = rtpProxyPort;
    }

    public int getRtspPort() {
        return rtspPort;
    }

    @Override
    public void setRtspPort(int rtspPort) {

    }

    public void setRtspPort(Integer rtspPort) {
        this.rtspPort = rtspPort;
    }

    public int getRtspSSLPort() {
        return rtspSSLPort;
    }

    @Override
    public void setRtspSSLPort(int rtspSSLPort) {

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

    public int getRecordAssistPort() {
        return recordAssistPort;
    }

    @Override
    public void setRecordAssistPort(int recordAssistPort) {

    }

    public void setRecordAssistPort(Integer recordAssistPort) {
        this.recordAssistPort = recordAssistPort;
    }

    @Override
    public boolean isDocker() {
        return docker;
    }

    @Override
    public void setDocker(boolean docker) {
        this.docker = docker;
    }

    public String getSdpIp() {
        if (StringUtils.isEmpty(sdpIp)){
            return ip;
        }else {
            return sdpIp;
        }
    }

    public String getStreamIp() {
        if (StringUtils.isEmpty(streamIp)){
            return ip;
        }else {
            return streamIp;
        }
    }



    public MediaServerItem getMediaSerItem(){
        MediaServerItem mediaServerItem = new MediaServerItem();
        mediaServerItem.setId(id);
        mediaServerItem.setIp(ip);
        mediaServerItem.setDocker(true);
        mediaServerItem.setHookIp(hookIp);
        mediaServerItem.setSdpIp(sdpIp);
        mediaServerItem.setStreamIp(streamIp);
        mediaServerItem.setHttpPort(httpPort);
        mediaServerItem.setHttpSSlPort(httpSSlPort);
        mediaServerItem.setRtmpPort(rtmpPort);
        mediaServerItem.setRtmpSSlPort(rtmpSSlPort);
        mediaServerItem.setRtpProxyPort(rtpProxyPort);
        mediaServerItem.setRtspPort(rtspPort);
        mediaServerItem.setRtspSSLPort(rtspSSLPort);
        mediaServerItem.setAutoConfig(autoConfig);
        mediaServerItem.setSecret(secret);
        mediaServerItem.setStreamNoneReaderDelayMS(streamNoneReaderDelayMS);
        mediaServerItem.setRtpEnable(rtpEnable);
        mediaServerItem.setRtpPortRange(rtpPortRange);
        mediaServerItem.setRecordAssistPort(recordAssistPort);
        mediaServerItem.setCreateTime(createTime);
        mediaServerItem.setUpdateTime(updateTime);
        mediaServerItem.setCount(count);
        return mediaServerItem;
    }

    @Override
    public String getUpdateTime() {
        return updateTime;
    }

    @Override
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void setCount(int count) {
        this.count = count;
    }
}
