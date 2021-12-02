package com.genersoft.iot.vmp.media.zlm.dto;

public enum OriginType {
    UNKNOWN("UNKNOWN"),
    RTMP_PUSH("PUSH"),
    RTSP_PUSH("PUSH"),
    RTP_PUSH("RTP"),
    RTC_PUSH("PUSH"),
    PULL("PULL"),
    FFMPEG_PULL("PULL"),
    MP4_VOD("MP4_VOD"),
    DEVICE_CHN("DEVICE_CHN");

    private final String type;
    OriginType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
