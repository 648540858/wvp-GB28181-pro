package com.genersoft.iot.vmp.service.bean;

/**
 * redis消息：请求下级推送流信息
 * @author lin
 */
public class RequestPushStreamMsg {


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
     * 是否使用TCP方式
     */
    private boolean tcp;

    /**
     * 本地使用的端口
     */
    private int srcPort;

    /**
     * 发送时，rtp的pt（uint8_t）,不传时默认为96
     */
    private int pt;

    /**
     * 发送时，rtp的负载类型。为true时，负载为ps；为false时，为es；
     */
    private boolean ps;

    /**
     * 是否只有音频
     */
    private boolean onlyAudio;


    public static RequestPushStreamMsg getInstance(String mediaServerId, String app, String stream, String ip, int port, String ssrc,
                                boolean tcp, int srcPort, int pt, boolean ps, boolean onlyAudio) {
        RequestPushStreamMsg requestPushStreamMsg = new RequestPushStreamMsg();
        requestPushStreamMsg.setMediaServerId(mediaServerId);
        requestPushStreamMsg.setApp(app);
        requestPushStreamMsg.setStream(stream);
        requestPushStreamMsg.setIp(ip);
        requestPushStreamMsg.setPort(port);
        requestPushStreamMsg.setSsrc(ssrc);
        requestPushStreamMsg.setTcp(tcp);
        requestPushStreamMsg.setSrcPort(srcPort);
        requestPushStreamMsg.setPt(pt);
        requestPushStreamMsg.setPs(ps);
        requestPushStreamMsg.setOnlyAudio(onlyAudio);
        return requestPushStreamMsg;
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

    public boolean isTcp() {
        return tcp;
    }

    public void setTcp(boolean tcp) {
        this.tcp = tcp;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getPt() {
        return pt;
    }

    public void setPt(int pt) {
        this.pt = pt;
    }

    public boolean isPs() {
        return ps;
    }

    public void setPs(boolean ps) {
        this.ps = ps;
    }

    public boolean isOnlyAudio() {
        return onlyAudio;
    }

    public void setOnlyAudio(boolean onlyAudio) {
        this.onlyAudio = onlyAudio;
    }
}
