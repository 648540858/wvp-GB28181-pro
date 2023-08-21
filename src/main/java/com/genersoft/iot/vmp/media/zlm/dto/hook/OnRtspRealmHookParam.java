package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_rtsp_realm事件的参数
 * @author AlphaWu
 */
public class OnRtspRealmHookParam extends HookParam{
    private String app;
    private String id;
    private String ip;
    private String params;
    private int port;
    private String schema;
    private String stream;
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

    @Override
    public String toString() {
        return "OnRtspRealmHookParam{" +
                "mediaServerId='" + super.getMediaServerId() + '\'' +
                "app='" + app + '\'' +
                ", id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", params='" + params + '\'' +
                ", port=" + port +
                ", schema='" + schema + '\'' +
                ", stream='" + stream + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
