package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;
import lombok.Data;

/**
 * 设备信息查询响应
 *
 * @author Y.G
 * @version 1.0
 * @date 2022/6/28 14:55
 */
@Data
public class HomePositionRequest {
    /**
     * 序列号
     */
    @MessageElement("SN")
    private String sn;

    @MessageElement("DeviceID")
    private String deviceId;

    @MessageElement(value = "HomePosition")
    private HomePosition homePosition;


    /**
     * 基本参数
     */
    @Data
    public static class HomePosition {
        /**
         * 播放窗口长度像素值
         */
        @MessageElement("Enabled")
        protected String enabled;
        /**
         * 播放窗口宽度像素值
         */
        @MessageElement("ResetTime")
        protected String resetTime;
        /**
         * 拉框中心的横轴坐标像素值
         */
        @MessageElement("PresetIndex")
        protected String presetIndex;

    }
}
