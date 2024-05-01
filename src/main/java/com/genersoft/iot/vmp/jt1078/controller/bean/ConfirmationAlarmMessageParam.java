package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JConfirmationAlarmMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 人工确认报警消息参数
 */
@Schema(description = "人工确认报警消息参数")
public class ConfirmationAlarmMessageParam {

    @Schema(description = "设备")
    private String deviceId;
    @Schema(description = "报警消息流水号")
    private int alarmPackageNo;
    @Schema(description = "人工确认报警类型")
    private JConfirmationAlarmMessageType alarmMessageType;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public JConfirmationAlarmMessageType getAlarmMessageType() {
        return alarmMessageType;
    }

    public void setAlarmMessageType(JConfirmationAlarmMessageType alarmMessageType) {
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
                "deviceId='" + deviceId + '\'' +
                ", alarmPackageNo=" + alarmPackageNo +
                ", alarmMessageType=" + alarmMessageType +
                '}';
    }
}
