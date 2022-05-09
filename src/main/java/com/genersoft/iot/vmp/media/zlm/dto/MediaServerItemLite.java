package com.genersoft.iot.vmp.media.zlm.dto;


import com.genersoft.iot.vmp.gb28181.session.SsrcConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import org.springframework.util.StringUtils;

import java.util.HashMap;

/**
 * 精简的MediaServerItem信息，方便给前端返回数据
 */
public class MediaServerItemLite {

    private String id;

    private String ip;

    private String hookIp;

    private String sdpIp;

    private String streamIp;

    private int httpPort;

    private int httpSSlPort;

    private int rtmpPort;

    private int rtmpSSlPort;

    private int rtpProxyPort;

    private int rtspPort;

    private int rtspSSLPort;

    private String secret;

    private int streamNoneReaderDelayMS;

    private int hookAliveInterval;

    private int recordAssistPort;



    public MediaServerItemLite(MediaServerItem mediaServerItem) {
        this.id = mediaServerItem.getId();
        this.ip = mediaServerItem.getIp();
        this.hookIp = mediaServerItem.getHookIp();
        this.sdpIp = mediaServerItem.getSdpIp();
        this.streamIp = mediaServerItem.getStreamIp();
        this.httpPort = mediaServerItem.getHttpPort();
        this.httpSSlPort = mediaServerItem.getHttpSSlPort();
        this.rtmpPort = mediaServerItem.getRtmpPort();
        this.rtmpSSlPort = mediaServerItem.getRtmpSSlPort();
        this.rtpProxyPort = mediaServerItem.getRtpProxyPort();
        this.rtspPort = mediaServerItem.getRtspPort();
        this.rtspSSLPort = mediaServerItem.getRtspSSLPort();
        this.secret = mediaServerItem.getSecret();
        this.streamNoneReaderDelayMS = mediaServerItem.getStreamNoneReaderDelayMS();
        this.hookAliveInterval = mediaServerItem.getHookAliveInterval();
        this.streamNoneReaderDelayMS = mediaServerItem.getStreamNoneReaderDelayMS();
        this.recordAssistPort = mediaServerItem.getRecordAssistPort();
    }

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
        return hookIp;
    }

    public void setHookIp(String hookIp) {
        this.hookIp = hookIp;
    }

    public String getSdpIp() {
        return sdpIp;
    }

    public void setSdpIp(String sdpIp) {
        this.sdpIp = sdpIp;
    }

    public String getStreamIp() {
        return streamIp;
    }

    public void setStreamIp(String streamIp) {
        this.streamIp = streamIp;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpSSlPort() {
        return httpSSlPort;
    }

    public void setHttpSSlPort(int httpSSlPort) {
        this.httpSSlPort = httpSSlPort;
    }

    public int getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(int rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public int getRtmpSSlPort() {
        return rtmpSSlPort;
    }

    public void setRtmpSSlPort(int rtmpSSlPort) {
        this.rtmpSSlPort = rtmpSSlPort;
    }

    public int getRtpProxyPort() {
        return rtpProxyPort;
    }

    public void setRtpProxyPort(int rtpProxyPort) {
        this.rtpProxyPort = rtpProxyPort;
    }

    public int getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(int rtspPort) {
        this.rtspPort = rtspPort;
    }

    public int getRtspSSLPort() {
        return rtspSSLPort;
    }

    public void setRtspSSLPort(int rtspSSLPort) {
        this.rtspSSLPort = rtspSSLPort;
    }


    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getStreamNoneReaderDelayMS() {
        return streamNoneReaderDelayMS;
    }

    public void setStreamNoneReaderDelayMS(int streamNoneReaderDelayMS) {
        this.streamNoneReaderDelayMS = streamNoneReaderDelayMS;
    }

    public int getHookAliveInterval() {
        return hookAliveInterval;
    }

    public void setHookAliveInterval(int hookAliveInterval) {
        this.hookAliveInterval = hookAliveInterval;
    }

    public int getRecordAssistPort() {
        return recordAssistPort;
    }

    public void setRecordAssistPort(int recordAssistPort) {
        this.recordAssistPort = recordAssistPort;
    }
}
