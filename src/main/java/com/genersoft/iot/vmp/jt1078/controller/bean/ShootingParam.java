package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "摄像头立即拍摄命令参数")
public class ShootingParam {

    @Schema(description = "设备")
    private String phoneNumber;

    @Schema(description = "拍摄命令参数")
    private JTShootingCommand shootingCommand;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
                "设备手机号='" + phoneNumber + '\'' +
                ", shootingCommand=" + shootingCommand +
                '}';
    }
}
