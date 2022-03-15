package com.genersoft.iot.vmp.media.zlm.dto;


import com.genersoft.iot.vmp.gb28181.session.SsrcConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import org.springframework.util.StringUtils;

import java.util.HashMap;

public class MediaServerItem{

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

    private boolean autoConfig;

    private String secret;

    private int streamNoneReaderDelayMS;

    private int hookAliveInterval;

    private boolean rtpEnable;

    private boolean status;

    private String rtpPortRange;

    private String sendRtpPortRange;

    private int recordAssistPort;

    private String createTime;

    private String updateTime;

    private String lastKeepaliveTime;

    private boolean defaultServer;

    private SsrcConfig ssrcConfig;

    private int currentPort;


    /**
     * 每一台ZLM都有一套独立的SSRC列表
     * 在ApplicationCheckRunner里对mediaServerSsrcMap进行初始化
     */
    private HashMap<String, SsrcConfig> mediaServerSsrcMap;

    public MediaServerItem() {
    }

    public MediaServerItem(ZLMServerConfig zlmServerConfig, String sipIp) {
        id = zlmServerConfig.getGeneralMediaServerId();
        ip = zlmServerConfig.getIp();
        hookIp = StringUtils.isEmpty(zlmServerConfig.getHookIp())? sipIp: zlmServerConfig.getHookIp();
        sdpIp = StringUtils.isEmpty(zlmServerConfig.getSdpIp())? zlmServerConfig.getIp(): zlmServerConfig.getSdpIp();
        streamIp = StringUtils.isEmpty(zlmServerConfig.getStreamIp())? zlmServerConfig.getIp(): zlmServerConfig.getStreamIp();
        httpPort = zlmServerConfig.getHttpPort();
        httpSSlPort = zlmServerConfig.getHttpSSLport();
        rtmpPort = zlmServerConfig.getRtmpPort();
        rtmpSSlPort = zlmServerConfig.getRtmpSslPort();
        rtpProxyPort = zlmServerConfig.getRtpProxyPort();
        rtspPort = zlmServerConfig.getRtspPort();
        rtspSSLPort = zlmServerConfig.getRtspSSlport();
        autoConfig = true; // 默认值true;
        secret = zlmServerConfig.getApiSecret();
        streamNoneReaderDelayMS = zlmServerConfig.getGeneralStreamNoneReaderDelayMS();
        hookAliveInterval = zlmServerConfig.getHookAliveInterval();
        rtpEnable = false; // 默认使用单端口;直到用户自己设置开启多端口
        rtpPortRange = zlmServerConfig.getPortRange().replace("_",","); // 默认使用30000,30500作为级联时发送流的端口号
        sendRtpPortRange = "30000,30500"; // 默认使用30000,30500作为级联时发送流的端口号
        recordAssistPort = 0; // 默认关闭

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

    public int getStreamNoneReaderDelayMS() {
        return streamNoneReaderDelayMS;
    }

    public void setStreamNoneReaderDelayMS(int streamNoneReaderDelayMS) {
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

    public void setRecordAssistPort(int recordAssistPort) {
        this.recordAssistPort = recordAssistPort;
    }

    public boolean isDefaultServer() {
        return defaultServer;
    }

    public void setDefaultServer(boolean defaultServer) {
        this.defaultServer = defaultServer;
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

    public HashMap<String, SsrcConfig> getMediaServerSsrcMap() {
        return mediaServerSsrcMap;
    }

    public void setMediaServerSsrcMap(HashMap<String, SsrcConfig> mediaServerSsrcMap) {
        this.mediaServerSsrcMap = mediaServerSsrcMap;
    }

    public SsrcConfig getSsrcConfig() {
        return ssrcConfig;
    }

    public void setSsrcConfig(SsrcConfig ssrcConfig) {
        this.ssrcConfig = ssrcConfig;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public void setCurrentPort(int currentPort) {
        this.currentPort = currentPort;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getLastKeepaliveTime() {
        return lastKeepaliveTime;
    }

    public void setLastKeepaliveTime(String lastKeepaliveTime) {
        this.lastKeepaliveTime = lastKeepaliveTime;
    }

    public String getSendRtpPortRange() {
        return sendRtpPortRange;
    }

    public void setSendRtpPortRange(String sendRtpPortRange) {
        this.sendRtpPortRange = sendRtpPortRange;
    }

    public int getHookAliveInterval() {
        return hookAliveInterval;
    }

    public void setHookAliveInterval(int hookAliveInterval) {
        this.hookAliveInterval = hookAliveInterval;
    }
}
