package com.genersoft.iot.vmp.service.bean;

import lombok.Data;

@Data
public class SSRCInfo {

    private int port;
    private String ssrc;
    private String app;
    private String Stream;
    private String timeOutTaskKey;

    public SSRCInfo(int port, String ssrc, String app, String stream, String timeOutTaskKey) {
        this.port = port;
        this.ssrc = ssrc;
        this.app = app;
        this.Stream = stream;
        this.timeOutTaskKey = timeOutTaskKey;
    }
}
