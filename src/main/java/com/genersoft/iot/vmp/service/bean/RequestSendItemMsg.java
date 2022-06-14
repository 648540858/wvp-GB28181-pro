package com.genersoft.iot.vmp.service.bean;

/**
 * redis消息：请求下级回复推送信息
 * @author lin
 */
public class RequestSendItemMsg {

    /**
     * 下级服务ID
     */
    private String serverId;

    /**
     * 下级服务ID
     */
    private String mediaServerId;

    /**
     * 流ID
     */
    private String app;

    /**
     * 应用名
     */
    private String stream;

    /**
     * 目标IP
     */
    private String ip;

    /**
     * 目标端口
     */
    private int port;

    /**
     * ssrc
     */
    private String ssrc;

    /**
     * 平台国标编号
     */
    private String platformId;

    /**
     * 平台名称
     */
    private String platformName;

    /**
     * 通道ID
     */
    private String channelId;


    /**
     * 是否使用TCP
     */
    private Boolean isTcp;




    public static RequestSendItemMsg getInstance(String serverId, String mediaServerId, String app, String stream, String ip, int port,
                                                          String ssrc, String platformId, String channelId, Boolean isTcp, String platformName) {
        RequestSendItemMsg requestSendItemMsg = new RequestSendItemMsg();
        requestSendItemMsg.setServerId(serverId);
        requestSendItemMsg.setMediaServerId(mediaServerId);
        requestSendItemMsg.setApp(app);
        requestSendItemMsg.setStream(stream);
        requestSendItemMsg.setIp(ip);
        requestSendItemMsg.setPort(port);
        requestSendItemMsg.setSsrc(ssrc);
        requestSendItemMsg.setPlatformId(platformId);
        requestSendItemMsg.setPlatformName(platformName);
        requestSendItemMsg.setChannelId(channelId);
        requestSendItemMsg.setTcp(isTcp);

        return  requestSendItemMsg;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
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

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Boolean getTcp() {
        return isTcp;
    }

    public void setTcp(Boolean tcp) {
        isTcp = tcp;
    }
}
