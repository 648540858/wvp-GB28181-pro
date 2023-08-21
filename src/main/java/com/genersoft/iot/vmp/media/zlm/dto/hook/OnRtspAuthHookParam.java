package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_rtsp_auth事件的参数
 * @author AlphaWu
 */
public class OnRtspAuthHookParam extends HookParam{
    private String app;
    private String id;
    private String ip;
    private boolean must_no_encrypt;
    private String params;
    private int port;
    private String realm;
    private String schema;
    private String stream;
    private String user_name;
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

    public boolean isMust_no_encrypt() {
        return must_no_encrypt;
    }

    public void setMust_no_encrypt(boolean must_no_encrypt) {
        this.must_no_encrypt = must_no_encrypt;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    @Override
    public String toString() {
        return "OnRtspAuthHookParam{" +
                "mediaServerId='" + super.getMediaServerId() + '\'' +
                "app='" + app + '\'' +
                ", id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", must_no_encrypt=" + must_no_encrypt +
                ", params='" + params + '\'' +
                ", port=" + port +
                ", realm='" + realm + '\'' +
                ", schema='" + schema + '\'' +
                ", stream='" + stream + '\'' +
                ", user_name='" + user_name + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
