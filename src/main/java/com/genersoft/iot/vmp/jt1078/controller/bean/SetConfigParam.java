package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "终端参数设置")
public class SetConfigParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;

    @Schema(description = "终端参数设置")
    private JTDeviceConfig config;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public JTDeviceConfig getConfig() {
        return config;
    }

    public void setConfig(JTDeviceConfig config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "SetConfigParam{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", config=" + config +
                '}';
    }
}
