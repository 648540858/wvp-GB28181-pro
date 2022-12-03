package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_rtsp_realm事件的参数
 * @author AlphaWu
 */
public class OnRtspRealmHookParam extends HookParam{
    /**
     * 流应用名
     */
    private String app;
    /**
     * TCP链接唯一ID
     */
    private String id;
    /**
     * rtsp播放器ip
     */
    private String ip;
    /**
     * rtsp url参数
     */
    private String params;
    /**
     * rtsp播放器端口号
     */
    private int port;
    /**
     * 	rtsp或rtsps
     */
    private String schema;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 流虚拟主机
     */
    private String vhost;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
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

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }
}
