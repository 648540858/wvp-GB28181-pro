package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_shell_login事件的参数
 * @author AlphaWu
 */
public class OnShellLoginHookParam extends HookParam{
    private String id;
    private String ip;
    private String passwd;
    private float port;
    private String user_name;

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

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public float getPort() {
        return port;
    }

    public void setPort(float port) {
        this.port = port;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    @Override
    public String toString() {
        return "OnShellLoginHookParam{" +
                "mediaServerId='" + super.getMediaServerId() + '\'' +
                ", id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", passwd='" + passwd + '\'' +
                ", port=" + port +
                ", user_name='" + user_name + '\'' +
                '}';
    }
}
