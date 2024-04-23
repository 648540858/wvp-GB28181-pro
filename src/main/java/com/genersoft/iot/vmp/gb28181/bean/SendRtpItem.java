package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;

import com.genersoft.iot.vmp.common.VideoManagerConstants;

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
     * 平台名称
     */
    private String platformName;

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
    private String stream;

    /**
     * 是否为tcp
     */
    private boolean tcp;

    /**
     * 是否为tcp主动模式
     */
    private boolean tcpActive;

    /**
     * 自己推流使用的IP
     */
    private String localIp;

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
     *  invite 的 callId
     */
    private String callId;

    /**
     *  invite 的 fromTag
     */
    private String fromTag;

    /**
     *  invite 的 toTag
     */
    private String toTag;

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
     * 是否开启rtcp保活
     */
    private boolean rtcp = false;


    /**
     * 播放类型
     */
    private InviteStreamType playType;

    /**
     * 发流的同时收流
     */
    private String receiveStream;

    /**
     * 上级的点播类型
     */
    private String sessionName;

    public static SendRtpItem getInstance(RequestPushStreamMsg requestPushStreamMsg) {
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setMediaServerId(requestPushStreamMsg.getMediaServerId());
        sendRtpItem.setApp(requestPushStreamMsg.getApp());
        sendRtpItem.setStream(requestPushStreamMsg.getStream());
        sendRtpItem.setIp(requestPushStreamMsg.getIp());
        sendRtpItem.setPort(requestPushStreamMsg.getPort());
        sendRtpItem.setSsrc(requestPushStreamMsg.getSsrc());
        sendRtpItem.setTcp(requestPushStreamMsg.isTcp());
        sendRtpItem.setLocalPort(requestPushStreamMsg.getSrcPort());
        sendRtpItem.setPt(requestPushStreamMsg.getPt());
        sendRtpItem.setUsePs(requestPushStreamMsg.isPs());
        sendRtpItem.setOnlyAudio(requestPushStreamMsg.isOnlyAudio());
        return sendRtpItem;

    }

    public static SendRtpItem getInstance(String app, String stream, String ssrc, String dstIp, Integer dstPort, boolean tcp, int sendLocalPort, Integer pt) {
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setApp(app);
        sendRtpItem.setStream(stream);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setLocalPort(sendLocalPort);
        sendRtpItem.setIp(dstIp);
        sendRtpItem.setPort(dstPort);
        if (pt != null) {
            sendRtpItem.setPt(pt);
        }

        return sendRtpItem;
    }

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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
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
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public InviteStreamType getPlayType() {
        return playType;
    }

    public void setPlayType(InviteStreamType playType) {
        this.playType = playType;
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

    public String getFromTag() {
        return fromTag;
    }

    public void setFromTag(String fromTag) {
        this.fromTag = fromTag;
    }

    public String getToTag() {
        return toTag;
    }

    public void setToTag(String toTag) {
        this.toTag = toTag;
    }

    public boolean isRtcp() {
        return rtcp;
    }

    public void setRtcp(boolean rtcp) {
        this.rtcp = rtcp;
    }

    public String getReceiveStream() {
        return receiveStream;
    }

    public void setReceiveStream(String receiveStream) {
        this.receiveStream = receiveStream;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    @Override
    public String toString() {
        return "SendRtpItem{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", ssrc='" + ssrc + '\'' +
                ", platformId='" + platformId + '\'' +
                ", platformName='" + platformName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", app='" + app + '\'' +
                ", channelId='" + channelId + '\'' +
                ", status=" + status +
                ", stream='" + stream + '\'' +
                ", tcp=" + tcp +
                ", tcpActive=" + tcpActive +
                ", localIp='" + localIp + '\'' +
                ", localPort=" + localPort +
                ", mediaServerId='" + mediaServerId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", CallId='" + callId + '\'' +
                ", fromTag='" + fromTag + '\'' +
                ", toTag='" + toTag + '\'' +
                ", pt=" + pt +
                ", usePs=" + usePs +
                ", onlyAudio=" + onlyAudio +
                ", rtcp=" + rtcp +
                ", playType=" + playType +
                ", receiveStream='" + receiveStream + '\'' +
                ", sessionName='" + sessionName + '\'' +
                '}';
    }

    public String getRedisKey() {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX +
                serverId + "_"
                + mediaServerId + "_"
                + platformId + "_"
                + channelId + "_"
                + stream + "_"
                + callId;
        return key;
    }
}
