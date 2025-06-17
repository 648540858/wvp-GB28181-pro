package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.PaginationRequest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonQuery extends PaginationRequest {

    @ApiModelProperty("设备编码")
    private String deviceId;
    @ApiModelProperty("开始时间")
    private String startTime;
    @ApiModelProperty("结束时间")
    private String endTime;
}
