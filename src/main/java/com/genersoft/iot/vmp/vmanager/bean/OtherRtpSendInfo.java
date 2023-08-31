package com.genersoft.iot.vmp.vmanager.bean;

public class OtherRtpSendInfo {

    /**
     * 发流IP
     */
    private String sendLocalIp;

    /**
     * 音频发流端口
     */
    private int sendLocalPortForAudio;

    /**
     * 视频发流端口
     */
    private int sendLocalPortForVideo;

    /**
     * 收流IP
     */
    private String receiveIp;

    /**
     * 音频收流端口
     */
    private int receivePortForAudio;

    /**
     * 视频收流端口
     */
    private int receivePortForVideo;

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


    public String getReceiveIp() {
        return receiveIp;
    }

    public void setReceiveIp(String receiveIp) {
        this.receiveIp = receiveIp;
    }

    public int getReceivePortForAudio() {
        return receivePortForAudio;
    }

    public void setReceivePortForAudio(int receivePortForAudio) {
        this.receivePortForAudio = receivePortForAudio;
    }

    public int getReceivePortForVideo() {
        return receivePortForVideo;
    }

    public void setReceivePortForVideo(int receivePortForVideo) {
        this.receivePortForVideo = receivePortForVideo;
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


    public String getSendLocalIp() {
        return sendLocalIp;
    }

    public void setSendLocalIp(String sendLocalIp) {
        this.sendLocalIp = sendLocalIp;
    }

    public int getSendLocalPortForAudio() {
        return sendLocalPortForAudio;
    }

    public void setSendLocalPortForAudio(int sendLocalPortForAudio) {
        this.sendLocalPortForAudio = sendLocalPortForAudio;
    }

    public int getSendLocalPortForVideo() {
        return sendLocalPortForVideo;
    }

    public void setSendLocalPortForVideo(int sendLocalPortForVideo) {
        this.sendLocalPortForVideo = sendLocalPortForVideo;
    }

    @Override
    public String toString() {
        return "OtherRtpSendInfo{" +
                "sendLocalIp='" + sendLocalIp + '\'' +
                ", sendLocalPortForAudio=" + sendLocalPortForAudio +
                ", sendLocalPortForVideo=" + sendLocalPortForVideo +
                ", receiveIp='" + receiveIp + '\'' +
                ", receivePortForAudio=" + receivePortForAudio +
                ", receivePortForVideo=" + receivePortForVideo +
                ", callId='" + callId + '\'' +
                ", stream='" + stream + '\'' +
                ", pushApp='" + pushApp + '\'' +
                ", pushStream='" + pushStream + '\'' +
                ", pushSSRC='" + pushSSRC + '\'' +
                '}';
    }
}
