package com.genersoft.iot.vmp.media.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultForOnPublish {

    private Boolean enable_audio;
    private Boolean enable_mp4;
    private Integer mp4_max_second;
    private String mp4_save_path;
    private String stream_replace;
    private Integer modify_stamp;
}
