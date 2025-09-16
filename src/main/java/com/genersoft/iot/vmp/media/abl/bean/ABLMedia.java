package com.genersoft.iot.vmp.media.abl.bean;

import lombok.Data;

@Data
public class ABLMedia {
    private String key;
    private String app;
    private String stream;
    private Integer sourceType;
    private Long duration;
    private String sim;
    private Boolean status;
    private Boolean enable_hls;
    private Boolean transcodingStatus;
    private String sourceURL;
    private Integer networkType;
    private Integer readerCount;
    private String videoCodec;
    private Integer width;
    private Integer height;
    private String audioCodec;
    private Integer audioChannels;
    private Integer audioSampleRate;
}
