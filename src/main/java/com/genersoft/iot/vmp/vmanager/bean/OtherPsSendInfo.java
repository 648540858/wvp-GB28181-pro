package com.genersoft.iot.vmp.vmanager.bean;

public class OtherPsSendInfo {

    /**
     * 发流IP
     */
    private String sendLocalIp;

    /**
     * 发流端口
     */
    private int sendLocalPort;

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

    public String getSendLocalIp() {
        return sendLocalIp;
    }

    public void setSendLocalIp(String sendLocalIp) {
        this.sendLocalIp = sendLocalIp;
    }

    public int getSendLocalPort() {
        return sendLocalPort;
    }

    public void setSendLocalPort(int sendLocalPort) {
        this.sendLocalPort = sendLocalPort;
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
        return "OtherPsSendInfo{" +
                "sendLocalIp='" + sendLocalIp + '\'' +
                ", sendLocalPort=" + sendLocalPort +
                ", receiveIp='" + receiveIp + '\'' +
                ", receivePort=" + receivePort +
                ", callId='" + callId + '\'' +
                ", stream='" + stream + '\'' +
                ", pushApp='" + pushApp + '\'' +
                ", pushStream='" + pushStream + '\'' +
                ", pushSSRC='" + pushSSRC + '\'' +
                '}';
    }
}
