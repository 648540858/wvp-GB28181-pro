package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class TalkRtpInfo {

    /**
     * 应用名, 待推送给设备的流应用名
     */
    private String app;

    /**
     * 流id, 待推送给设备的流id
     */
    private String stream;

    /**
     * rtp推流出去的ssrc
     */
    private String ssrc;

    /**
     * 对方rtp推流上来的流id
     */
    private String receiveStreamId;

    /**
     * 是否推送本地MP4录像，该参数非必选参数
     */
    private Integer fromMp4;

    /**
     * 类型： 0(ES流)、1(PS流)、2(TS流)，默认1(PS流)；该参数非必选参数
     */
    private Integer type;

    /**
     * rtp payload type，默认96；该参数非必选参数
     */
    private Integer pt;

    /**
     * rtp es方式打包时，是否只打包音频；该参数非必选参数
     */
    private Integer onlyAudio;

    /**
     * 转发rtp(tcp模式)时，如果发送不出去，是否限制源端收流速度，此参数在多倍速rtp转发时作用较大
     */
    private Integer enableOriginReceiveLimit;

}
