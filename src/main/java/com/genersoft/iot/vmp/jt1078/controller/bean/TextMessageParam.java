package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JConfirmationAlarmMessageType;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文本信息下发参数
 */
@Schema(description = "人工确认报警消息参数")
public class TextMessageParam {

    @Schema(description = "设备")
    private String deviceId;
    @Schema(description = "标志")
    private JTTextSign sign;
    @Schema(description = "文本类型,1 = 通知 ，2 = 服务")
    private int textType;
    @Schema(description = "消息内容，最长为1024字节")
    private String content;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getTextType() {
        return textType;
    }

    public void setTextType(int textType) {
        this.textType = textType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public JTTextSign getSign() {
        return sign;
    }

    public void setSign(JTTextSign sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        return "TextMessageParam{" +
                "deviceId='" + deviceId + '\'' +
                ", sign=" + sign +
                ", textType=" + textType +
                ", content='" + content + '\'' +
                '}';
    }
}
