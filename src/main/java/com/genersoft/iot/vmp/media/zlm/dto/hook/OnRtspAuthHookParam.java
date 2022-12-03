package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_rtsp_auth事件的参数
 * @author AlphaWu
 */
public class OnRtspAuthHookParam extends HookParam{
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
     * 请求的密码是否必须为明文(base64鉴权需要明文密码)
     */
    private boolean must_no_encrypt;
    /**
     * rtsp url参数
     */
    private String params;
    /**
     * rtsp播放器端口号
     */
    private int port;
    /**
     * rtsp播放鉴权加密realm
     */
    private String realm;
    /**
     * 	rtsp或rtsps
     */
    private String schema;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 	播放用户名
     */
    private String user_name;
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
}
