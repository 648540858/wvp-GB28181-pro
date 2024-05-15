package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "存储多媒体数据参数")
public class QueryMediaDataParam {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "存储多媒体数据参数")
    private JTQueryMediaDataCommand queryMediaDataCommand;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public JTQueryMediaDataCommand getQueryMediaDataCommand() {
        return queryMediaDataCommand;
    }

    public void setQueryMediaDataCommand(JTQueryMediaDataCommand queryMediaDataCommand) {
        this.queryMediaDataCommand = queryMediaDataCommand;
    }

    @Override
    public String toString() {
        return "QueryMediaDataParam{" +
                "deviceId='" + deviceId + '\'' +
                ", queryMediaDataCommand=" + queryMediaDataCommand +
                '}';
    }
}
