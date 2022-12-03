package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_shell_login事件的参数
 * @author AlphaWu
 */
public class OnShellLoginHookParam extends HookParam{
    /**
     * TCP链接唯一ID
     */
    private String id;
    /**
     * telnet 终端ip
     */
    private String ip;
    /**
     * telnet 终端登录用户密码
     */
    private String passwd;
    /**
     * telnet 终端端口号
     */
    private int port;
    /**
     * telnet 终端登录用户名
     */
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }
}
