package com.genersoft.iot.vmp.gb28181.bean;

public class SendRtpItem {

    /**
     * 推流ip
     */
    private String ip;

    /**
     * 推流端口
     */
    private int port;

    /**
     * 推流标识
     */
    private String ssrc;

    /**
     * 平台id
     */
    private String platformId;

     /**
     * 对应设备id
     */
    private String deviceId;

    /**
     * 直播流的应用名
     */
    private String app;

   /**
     * 通道id
     */
    private String channelId;

    /**
     * 推流状态
     * 0 等待设备推流上来
     * 1 等待上级平台回复ack
     * 2 推流中
     */
    private int status = 0;


    /**
     * 设备推流的streamId
     */
    private String streamId;

    /**
     * 是否为tcp
     */
    private boolean tcp;

    /**
     * 是否为tcp主动模式
     */
    private boolean tcpActive;

    /**
     * 自己推流使用的端口
     */
    private int localPort;

    /**
     * 使用的流媒体
     */
    private String mediaServerId;

    /**
     * 使用的服务的ID
     */
    private String serverId;

    /**
     *  invite的callId
     */
    private String CallId;

    /**
     * 发送时，rtp的pt（uint8_t）,不传时默认为96
     */
    private int pt = 96;

    /**
     * 发送时，rtp的负载类型。为true时，负载为ps；为false时，为es；
     */
    private boolean usePs = true;

    /**
     * 当usePs 为false时，有效。为1时，发送音频；为0时，发送视频；不传时默认为0
     */
    private boolean onlyAudio = false;

    /**
     * 播放类型
     */
    private InviteStreamType playType;

    private byte[] transaction;

    private byte[] dialog;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public boolean isTcp() {
        return tcp;
    }

    public void setTcp(boolean tcp) {
        this.tcp = tcp;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public boolean isTcpActive() {
        return tcpActive;
    }

    public void setTcpActive(boolean tcpActive) {
        this.tcpActive = tcpActive;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getCallId() {
        return CallId;
    }

    public void setCallId(String callId) {
        CallId = callId;
    }

    public InviteStreamType getPlayType() {
        return playType;
    }

    public void setPlayType(InviteStreamType playType) {
        this.playType = playType;
    }

    public byte[] getTransaction() {
        return transaction;
    }

    public void setTransaction(byte[] transaction) {
        this.transaction = transaction;
    }

    public byte[] getDialog() {
        return dialog;
    }

    public void setDialog(byte[] dialog) {
        this.dialog = dialog;
    }

    public int getPt() {
        return pt;
    }

    public void setPt(int pt) {
        this.pt = pt;
    }

    public boolean isUsePs() {
        return usePs;
    }

    public void setUsePs(boolean usePs) {
        this.usePs = usePs;
    }

    public boolean isOnlyAudio() {
        return onlyAudio;
    }

    public void setOnlyAudio(boolean onlyAudio) {
        this.onlyAudio = onlyAudio;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}
