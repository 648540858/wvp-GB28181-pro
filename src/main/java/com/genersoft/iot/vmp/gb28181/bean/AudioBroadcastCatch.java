package com.genersoft.iot.vmp.gb28181.bean;


import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPDialog;

import javax.sip.Dialog;

/**
 * 缓存语音广播的状态
 * @author lin
 */
public class AudioBroadcastCatch {


    public AudioBroadcastCatch(String deviceId, String channelId, AudioBroadcastCatchStatus status) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.status = status;
    }

    public AudioBroadcastCatch() {
    }

    /**
     * 设备编号
     */
    private String deviceId;

    /**
     * 通道编号
     */
    private String channelId;

    /**
     * 语音广播状态
     */
    private AudioBroadcastCatchStatus status;

    /**
     * 请求信息
     */
    private SIPRequest request;

    /**
     * 会话信息
     */
    private SIPDialog dialog;


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public AudioBroadcastCatchStatus getStatus() {
        return status;
    }

    public void setStatus(AudioBroadcastCatchStatus status) {
        this.status = status;
    }

    public void setDialog(SIPDialog dialog) {
        this.dialog = dialog;
    }

    public SIPDialog getDialog() {
        return dialog;
    }

    public SIPRequest getRequest() {
        return request;
    }

    public void setRequest(SIPRequest request) {
        this.request = request;
    }
}
