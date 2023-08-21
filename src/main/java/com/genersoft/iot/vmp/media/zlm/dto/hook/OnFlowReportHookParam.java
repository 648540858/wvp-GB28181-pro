package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_flow_report事件的参数
 * @author AlphaWu
 */
public class OnFlowReportHookParam extends HookParam{
    private String app;
    private int duration;
    private String params;
    private boolean player;
    private String schema;
    private String stream;
    private int totalBytes;
    private String vhost;
    private String ip;
    private int port;
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

    @Override
    public String toString() {
        return "OnFlowReportHookParam{" +
                "mediaServerId='" + super.getMediaServerId() + '\'' +
                ", app='" + app + '\'' +
                ", duration=" + duration +
                ", params='" + params + '\'' +
                ", player=" + player +
                ", schema='" + schema + '\'' +
                ", stream='" + stream + '\'' +
                ", totalBytes=" + totalBytes +
                ", vhost='" + vhost + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", id='" + id + '\'' +
                '}';
    }
}
