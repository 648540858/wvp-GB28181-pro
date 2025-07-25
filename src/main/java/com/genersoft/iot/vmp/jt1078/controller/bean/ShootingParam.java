package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "摄像头立即拍摄命令参数")
public class ShootingParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;

    @Schema(description = "拍摄命令参数")
    private JTShootingCommand shootingCommand;

    @Override
    public String toString() {
        return "ShootingParam{" +
                "设备手机号='" + phoneNumber + '\'' +
                ", shootingCommand=" + shootingCommand +
                '}';
    }
}
