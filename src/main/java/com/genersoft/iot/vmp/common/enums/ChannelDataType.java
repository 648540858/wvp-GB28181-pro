package com.genersoft.iot.vmp.common.enums;

/**
 * 支持的通道数据类型
 */

public enum ChannelDataType {

    GB28181(1,"国标28181"),
    STREAM_PUSH(2,"推流设备"),
    STREAM_PROXY(3,"拉流代理");

    public final int value;

    public final String desc;

    ChannelDataType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
