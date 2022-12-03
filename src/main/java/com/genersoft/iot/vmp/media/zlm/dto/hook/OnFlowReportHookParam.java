package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_flow_report事件的参数
 * @author AlphaWu
 */
public class OnFlowReportHookParam extends HookParam{
    /**
     * 流应用名
     */
    private String app;
    /**
     * tcp链接维持时间，单位秒
     */
    private int duration;
    /**
     * 推流或播放url参数
     */
    private String params;
    /**
     * true为播放器，false为推流器
     */
    private boolean player;
    /**
     * 	播放或推流的协议，可能是rtsp、rtmp、http
     */
    private String schema;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 	耗费上下行流量总和，单位字节
     */
    private int totalBytes;
    /**
     * 流虚拟主机
     */
    private String vhost;
    /**
     * 客户端ip
     */
    private String ip;
    /**
     * 客户端端口号
     */
    private int port;
    /**
     * TCP链接唯一ID
     */
    private String id;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public boolean isPlayer() {
        return player;
    }

    public void setPlayer(boolean player) {
        this.player = player;
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

    public int getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(int totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
