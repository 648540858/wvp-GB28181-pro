package com.genersoft.iot.vmp.common.enums;

/**
 * 支持的通道数据类型
 */

public class ChannelDataType {

    public final static int GB28181 = 1;
    public final static int STREAM_PUSH = 2;
    public final static int STREAM_PROXY = 3;
    public final static int JT_1078 = 200;

    public final static String PLAY_SERVICE = "sourceChannelPlayService";
    public final static String PLAYBACK_SERVICE = "sourceChannelPlaybackService";
    public final static String DOWNLOAD_SERVICE = "sourceChannelDownloadService";
    public final static String PTZ_SERVICE = "sourceChannelPTZService";


    public static String getDateTypeDesc(Integer dataType) {
        if (dataType == null) {
            return "未知";
        }
        return switch (dataType) {
            case ChannelDataType.GB28181 -> "国标28181";
            case ChannelDataType.STREAM_PUSH -> "推流设备";
            case ChannelDataType.STREAM_PROXY -> "拉流代理";
            case ChannelDataType.JT_1078 -> "部标设备";
            default -> "未知";
        };
    }


}
