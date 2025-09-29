package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通道信息")
public class ChannelParam {

    @Schema(description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    private String deviceCode;

    @Schema(description = "通道编号")
    private String deviceId;
}
