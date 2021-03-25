package com.genersoft.iot.vmp.common;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class StreamInfo {

    /**
     * zlm流媒体服务器IP
     */
    private String mediaServerIp;
    private String ssrc;
    private String streamId;
    private String deviceID;
    private String channelId;
    private String flv;
    private String ws_flv;
    private String fmp4;
    private String ws_fmp4;
    private String hls;
    private String ws_hls;
    private String ts;
    private String ws_ts;
    private String rtmp;
    private String rtsp;
    private JSONArray tracks;
}
