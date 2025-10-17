package com.genersoft.iot.vmp.web.custom.bean;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Schema(description = "摄像头信息")
public class CameraChannel extends CommonGBChannel {

    @Schema(description = "摄像头设备国标编号")
    private String deviceCode;


    @Schema(description = "图标路径")
    private String icon;

    /**
     * 分组别名
     */
    @Schema(description = "所属组织结构别名")
    private String groupAlias;

    /**
     * 分组所属业务分组别名
     */
    @Schema(description = "所属业务分组别名")
    private String topGroupGAlias;
}
