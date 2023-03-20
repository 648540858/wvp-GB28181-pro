package com.genersoft.iot.vmp.gb28181.bean;


import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import gov.nist.javax.sip.message.SIPResponse;

/**
 * 缓存语音广播的状态
 * @author lin
 */
public class AudioBroadcastCatch {


    public AudioBroadcastCatch(String deviceId,
                               String channelId,
                               AudioBroadcastCatchStatus status,
                               MediaServerItem mediaServerItem,
                               String app,
                               String stream) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.status = status;
        this.mediaServerItem = mediaServerItem;
        this.app = app;
        this.stream = stream;
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
     * 使用的流媒体
     */
    private MediaServerItem mediaServerItem;

    /**
     * 待推送给设备的流应用名
     */
    private String app;

    /**
     * 待推送给设备的流ID
     */
    private String stream;

    /**
     * 语音广播状态
     */
    private AudioBroadcastCatchStatus status;

    /**
     * 请求信息
     */
    private SipTransactionInfo sipTransactionInfo;


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

    public SipTransactionInfo getSipTransactionInfo() {
        return sipTransactionInfo;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public void setSipTransactionInfo(SipTransactionInfo sipTransactionInfo) {
        this.sipTransactionInfo = sipTransactionInfo;
    }

    public void setSipTransactionInfoByRequset(SIPResponse response) {
        this.sipTransactionInfo = new SipTransactionInfo(response, false);
    }

    public MediaServerItem getMediaServerItem() {
        return mediaServerItem;
    }

    public void setMediaServerItem(MediaServerItem mediaServerItem) {
        this.mediaServerItem = mediaServerItem;
    }
}
