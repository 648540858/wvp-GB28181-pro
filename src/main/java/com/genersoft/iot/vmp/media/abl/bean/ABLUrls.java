package com.genersoft.iot.vmp.media.abl.bean;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class ABLUrls {
    private String rtsp;
    private String rtmp;

    @JSONField(name = "http-flv")
    private String httpFlv;

    @JSONField(name = "ws-flv")
    private String wsFlv;

    @JSONField(name = "http-mp4")
    private String httpMp4;

    @JSONField(name = "http-hls")
    private String httpHls;

    private String download;
}
