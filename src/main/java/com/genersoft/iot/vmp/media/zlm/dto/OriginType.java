package com.genersoft.iot.vmp.media.zlm.dto;

public enum OriginType {
    // 不可调整顺序
    UNKNOWN("UNKNOWN"),
    RTMP_PUSH("PUSH"),
    RTSP_PUSH("PUSH"),
    RTP_PUSH("RTP"),
    PULL("PULL"),
    FFMPEG_PULL("PULL"),
    MP4_VOD("MP4_VOD"),
    DEVICE_CHN("DEVICE_CHN"),
    RTC_PUSH("PUSH");

    private final String type;
    OriginType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
