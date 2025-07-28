package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTConfirmationAlarmMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 人工确认报警消息参数
 */
@Setter
@Getter
@Schema(description = "人工确认报警消息参数")
public class ConfirmationAlarmMessageParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;
    @Schema(description = "报警消息流水号")
    private int alarmPackageNo;
    @Schema(description = "人工确认报警类型")
    private JTConfirmationAlarmMessageType alarmMessageType;

    @Override
    public String toString() {
        return "ConfirmationAlarmMessageParam{" +
                "PhoneNumber='" + phoneNumber + '\'' +
                ", alarmPackageNo=" + alarmPackageNo +
                ", alarmMessageType=" + alarmMessageType +
                '}';
    }
}
