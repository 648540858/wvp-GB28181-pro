package com.genersoft.iot.vmp.web.custom.bean;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


@Schema(description = "摄像头信息")
public class CameraChannel extends CommonGBChannel {

    @Getter
    @Setter
    @Schema(description = "摄像头设备国标编号")
    private String deviceCode;

    @Getter
    @Setter
    @Schema(description = "图标路径")
    private String icon;

    @Getter
    @Setter
    @Schema(description = "移动设备唯一编号")
    private Long unitNo;
}
