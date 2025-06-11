package com.genersoft.iot.vmp.gat1400.fontend.domain;

import cz.data.viid.framework.domain.core.PaginationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APEDeviceQuery extends PaginationRequest {

    @ApiModelProperty("设备标识")
    private String apeId;
    @ApiModelProperty("设备名称")
    private String name;
    @ApiModelProperty("地区编码")
    private String placeCode;
    @ApiModelProperty("是否在线")
    private String isOnline;
}
