package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTConfirmationAlarmMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 人工确认报警消息参数
 */
@Schema(description = "人工确认报警消息参数")
public class ConfirmationAlarmMessageParam {

    @Schema(description = "设备")
    private String phoneNumber;
    @Schema(description = "报警消息流水号")
    private int alarmPackageNo;
    @Schema(description = "人工确认报警类型")
    private JTConfirmationAlarmMessageType alarmMessageType;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String PhoneNumber) {
        this.phoneNumber = PhoneNumber;
    }

    public JTConfirmationAlarmMessageType getAlarmMessageType() {
        return alarmMessageType;
    }

    public void setAlarmMessageType(JTConfirmationAlarmMessageType alarmMessageType) {
        this.alarmMessageType = alarmMessageType;
    }

    public int getAlarmPackageNo() {
        return alarmPackageNo;
    }

    public void setAlarmPackageNo(int alarmPackageNo) {
        this.alarmPackageNo = alarmPackageNo;
    }

    @Override
    public String toString() {
        return "ConfirmationAlarmMessageParam{" +
                "PhoneNumber='" + phoneNumber + '\'' +
                ", alarmPackageNo=" + alarmPackageNo +
                ", alarmMessageType=" + alarmMessageType +
                '}';
    }
}
