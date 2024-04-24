package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;

public class SetConfigParam {

    private String deviceId;
    private JTDeviceConfig config;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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
                "deviceId='" + deviceId + '\'' +
                ", config=" + config +
                '}';
    }
}
