package com.genersoft.iot.vmp.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 支持的通道数据类型
 */

public class ChannelDataType {

    public final static int GB28181 = 1;
    public final static int STREAM_PUSH = 2;
    public final static int STREAM_PROXY = 3;

    public static Map<String, Integer> getDescMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("国标28181", ChannelDataType.GB28181);
        map.put("推流设备", ChannelDataType.STREAM_PUSH);
        map.put("拉流代理", ChannelDataType.STREAM_PROXY);
        return map;
    }

}
