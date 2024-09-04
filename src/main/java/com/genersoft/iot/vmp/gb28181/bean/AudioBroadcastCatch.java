package com.genersoft.iot.vmp.gb28181.bean;


import com.genersoft.iot.vmp.gb28181.controller.bean.AudioBroadcastEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;

/**
 * 缓存语音广播的状态
 * @author lin
 */
@Data
public class AudioBroadcastCatch {


    public AudioBroadcastCatch(
            String deviceId,
            Integer channelId,
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
    private Integer channelId;

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


    public void setSipTransactionInfoByRequest(SIPResponse sipResponse) {
        this.sipTransactionInfo = new SipTransactionInfo(sipResponse);
    }
}
