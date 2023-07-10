package com.genersoft.iot.vmp.vmanager.bean;

public class OtherRtpSendInfo {

    /**
     * 发流IP
     */
    private String ip;

    /**
     * 发流端口
     */
    private int port;

    /**
     * 收流IP
     */
    private String receiveIp;

    /**
     * 收流端口
     */
    private int receivePort;

    /**
     * 会话ID
     */
    private String callId;

    /**
     * 流ID
     */
    private String stream;

    /**
     * 推流应用名
     */
    private String pushApp;

    /**
     * 推流流ID
     */
    private String pushStream;

    /**
     * 推流SSRC
     */
    private String pushSSRC;



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

    public String getReceiveIp() {
        return receiveIp;
    }

    public void setReceiveIp(String receiveIp) {
        this.receiveIp = receiveIp;
    }

    public int getReceivePort() {
        return receivePort;
    }

    public void setReceivePort(int receivePort) {
        this.receivePort = receivePort;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getPushApp() {
        return pushApp;
    }

    public void setPushApp(String pushApp) {
        this.pushApp = pushApp;
    }

    public String getPushStream() {
        return pushStream;
    }

    public void setPushStream(String pushStream) {
        this.pushStream = pushStream;
    }

    public String getPushSSRC() {
        return pushSSRC;
    }

    public void setPushSSRC(String pushSSRC) {
        this.pushSSRC = pushSSRC;
    }

    @Override
    public String toString() {
        return "OtherRtpSendInfo{" +
                "  ip='" + ip + '\'' +
                ", port=" + port +
                ", receiveIp='" + receiveIp + '\'' +
                ", receivePort=" + receivePort +
                ", callId='" + callId + '\'' +
                ", stream='" + stream + '\'' +
                '}';
    }
}
