package com.genersoft.iot.vmp.common;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class StreamInfo {

    private String ssrc;
    private String deviceID;
    private String cahnnelId;
    private String flv;
    private String ws_flv;
    private String rtmp;
    private String hls;
    private String rtsp;
    private JSONArray tracks;

}
