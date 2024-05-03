package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTCircleArea;
import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "设置区域")
public class SetAreaParam {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "圆形区域项")
    private List<JTCircleArea> circleAreaList;


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<JTCircleArea> getCircleAreaList() {
        return circleAreaList;
    }

    public void setCircleAreaList(List<JTCircleArea> circleAreaList) {
        this.circleAreaList = circleAreaList;
    }

    @Override
    public String toString() {
        return "SetAreaParam{" +
                "deviceId='" + deviceId + '\'' +
                ", circleAreaList=" + circleAreaList +
                '}';
    }
}
