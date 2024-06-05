package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;

public class SetConfigParam {

    private String phoneNumber;
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
