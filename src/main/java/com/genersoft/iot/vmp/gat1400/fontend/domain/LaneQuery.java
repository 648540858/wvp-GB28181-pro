package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.PaginationRequest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LaneQuery extends PaginationRequest {

    @ApiModelProperty(value = "卡口编号")
    private String tollgateId;
    @ApiModelProperty(value = "车道ID")
    private Integer laneId;
    @ApiModelProperty(value = "车道名称")
    private String name;
    @ApiModelProperty(value = "车道描述")
    private String desc;
}
