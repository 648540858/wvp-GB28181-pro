package com.genersoft.iot.vmp.gat1400.fontend.domain;

import cz.data.viid.framework.domain.core.PaginationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MotorVehicleQuery extends PaginationRequest {

    @ApiModelProperty("设备编码")
    private String deviceId;
    @ApiModelProperty("开始时间")
    private String startTime;
    @ApiModelProperty("结束时间")
    private String endTime;
}
