package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "摄像头立即拍摄命令参数")
public class ShootingParam {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "拍摄命令参数")
    private JTShootingCommand shootingCommand;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public JTShootingCommand getShootingCommand() {
        return shootingCommand;
    }

    public void setShootingCommand(JTShootingCommand shootingCommand) {
        this.shootingCommand = shootingCommand;
    }

    @Override
    public String toString() {
        return "ShootingParam{" +
                "deviceId='" + deviceId + '\'' +
                ", shootingCommand=" + shootingCommand +
                '}';
    }
}
