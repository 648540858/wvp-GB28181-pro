package com.genersoft.iot.vmp.media.abl.bean.hook;

import com.genersoft.iot.vmp.media.abl.bean.AblUrls;
import lombok.Getter;
import lombok.Setter;

/**
 * 流到来的事件
 */
@Getter
@Setter
public class OnStreamArriveABLHookParam extends ABLHookParam{



    /**
     * 推流鉴权Id
     */
    private String callId;

    /**
     * 状态
     */
    private Boolean status;


    /**
     *
     */
    private Boolean enableHls;


    /**
     *
     */
    private Boolean transcodingStatus;


    /**
     *
     */
    private String sourceURL;


    /**
     *
     */
    private Integer readerCount;


    /**
     *
     */
    private Integer noneReaderDuration;


    /**
     *
     */
    private String videoCodec;


    /**
     *
     */
    private Integer videoFrameSpeed;


    /**
     *
     */
    private Integer width;


    /**
     *
     */
    private Integer height;


    /**
     *
     */
    private Integer videoBitrate;


    /**
     *
     */
    private String audioCodec;


    /**
     *
     */
    private Integer audioChannels;


    /**
     *
     */
    private Integer audioSampleRate;


    /**
     *
     */
    private Integer audioBitrate;


    private AblUrls url;
}
