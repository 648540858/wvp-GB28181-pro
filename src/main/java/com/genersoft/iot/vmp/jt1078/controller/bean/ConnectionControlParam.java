package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConnectionControl;

public class ConnectionControlParam {

    private String deviceId;
    private JTDeviceConnectionControl control;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public JTDeviceConnectionControl getControl() {
        return control;
    }

    public void setControl(JTDeviceConnectionControl control) {
        this.control = control;
    }

    @Override
    public String toString() {
        return "ConnectionControlParam{" +
                "deviceId='" + deviceId + '\'' +
                ", control=" + control +
                '}';
    }
}
