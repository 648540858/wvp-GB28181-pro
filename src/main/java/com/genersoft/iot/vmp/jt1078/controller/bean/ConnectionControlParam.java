package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConnectionControl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConnectionControlParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;
    private JTDeviceConnectionControl control;

    @Override
    public String toString() {
        return "ConnectionControlParam{" +
                "deviceId='" + phoneNumber + '\'' +
                ", control=" + control +
                '}';
    }
}
