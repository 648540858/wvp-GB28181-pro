package com.genersoft.iot.vmp.gat1400.fontend.domain;

import cz.data.viid.framework.domain.core.PaginationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TollgateQuery extends PaginationRequest {

    @ApiModelProperty("设备标识")
    private String tollgateId;
    @ApiModelProperty("卡口名称")
    private String name;
    @ApiModelProperty("组织机构编码")
    private String orgCode;
}
