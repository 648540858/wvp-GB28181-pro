package com.genersoft.iot.vmp.gb28181.bean;


import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.AudioBroadcastEvent;
import gov.nist.javax.sip.message.SIPResponse;

/**
 * 缓存语音广播的状态
 * @author lin
 */
public class AudioBroadcastCatch {


    public AudioBroadcastCatch(
            String deviceId,
            String channelId,
            MediaServer mediaServerItem,
            String app,
            String stream,
            AudioBroadcastEvent event,
            AudioBroadcastCatchStatus status,
            boolean isFromPlatform
    ) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.status = status;
        this.event = event;
        this.isFromPlatform = isFromPlatform;
        this.app = app;
        this.stream = stream;
        this.mediaServerItem = mediaServerItem;
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
     * 流媒体信息
     */
    private MediaServer mediaServerItem;

    /**
     * 关联的流APP
     */
    private String app;

    /**
     * 关联的流STREAM
     */
    private String stream;

    /**
     *  是否是级联语音喊话
     */
    private boolean isFromPlatform;

    /**
     * 语音广播状态
     */
    private AudioBroadcastCatchStatus status;

    /**
     * 请求信息
     */
    private SipTransactionInfo sipTransactionInfo;

    /**
     * 请求结果回调
     */
    private AudioBroadcastEvent event;


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

    public MediaServer getMediaServerItem() {
        return mediaServerItem;
    }

    public void setMediaServerItem(MediaServer mediaServerItem) {
        this.mediaServerItem = mediaServerItem;
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

    public boolean isFromPlatform() {
        return isFromPlatform;
    }

    public void setFromPlatform(boolean fromPlatform) {
        isFromPlatform = fromPlatform;
    }

    public void setSipTransactionInfo(SipTransactionInfo sipTransactionInfo) {
        this.sipTransactionInfo = sipTransactionInfo;
    }

    public AudioBroadcastEvent getEvent() {
        return event;
    }

    public void setEvent(AudioBroadcastEvent event) {
        this.event = event;
    }

    public void setSipTransactionInfoByRequset(SIPResponse sipResponse) {
        this.sipTransactionInfo = new SipTransactionInfo(sipResponse);
    }
}
