package com.genersoft.iot.vmp.sip.bean;

/**
 * sip系统的帐号信息
 */
public class SipServer {

    private int id;

    /**
     * 本机IP
     */
    private String localIp;

    /**
     * 本机端口
     */
    private Integer localPort;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 服务器端口
     */
    private Integer serverPort;

    /**
     * 信令传输模式， 默认UDP， 可选UDP/TCP
     */
    private String transport;

    /**
     * 状态
     */
    private boolean status;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;


    public static SipServer getInstance(String serverIp, Integer serverPort, String localIp, Integer localPort, String transport) {
        SipServer sipServer = new SipServer();
        sipServer.setTransport(transport);
        sipServer.setServerIp(serverIp);
        sipServer.setServerPort(serverPort);
        sipServer.setLocalIp(localIp);
        sipServer.setLocalPort(localPort);
        return sipServer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
