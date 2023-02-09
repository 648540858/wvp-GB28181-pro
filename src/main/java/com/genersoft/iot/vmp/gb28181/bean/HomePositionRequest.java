package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;

/**
 * 设备信息查询响应
 *
 * @author Y.G
 * @version 1.0
 * @date 2022/6/28 14:55
 */
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

        public String getEnabled() {
            return enabled;
        }

        public void setEnabled(String enabled) {
            this.enabled = enabled;
        }

        public String getResetTime() {
            return resetTime;
        }

        public void setResetTime(String resetTime) {
            this.resetTime = resetTime;
        }

        public String getPresetIndex() {
            return presetIndex;
        }

        public void setPresetIndex(String presetIndex) {
            this.presetIndex = presetIndex;
        }
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public HomePosition getHomePosition() {
        return homePosition;
    }

    public void setHomePosition(HomePosition homePosition) {
        this.homePosition = homePosition;
    }
}
