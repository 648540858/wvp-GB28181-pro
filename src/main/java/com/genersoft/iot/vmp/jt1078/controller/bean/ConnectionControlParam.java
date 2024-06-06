package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConnectionControl;
import io.swagger.v3.oas.annotations.media.Schema;

public class ConnectionControlParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;
    private JTDeviceConnectionControl control;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
                "deviceId='" + phoneNumber + '\'' +
                ", control=" + control +
                '}';
    }
}
