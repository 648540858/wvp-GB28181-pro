package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 文本信息下发参数
 */
@Setter
@Getter
@Schema(description = "人工确认报警消息参数")
public class TextMessageParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;
    @Schema(description = "标志")
    private JTTextSign sign;
    @Schema(description = "文本类型,1 = 通知 ，2 = 服务")
    private int textType;
    @Schema(description = "消息内容，最长为1024字节")
    private String content;

    @Override
    public String toString() {
        return "TextMessageParam{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", sign=" + sign +
                ", textType=" + textType +
                ", content='" + content + '\'' +
                '}';
    }
}
