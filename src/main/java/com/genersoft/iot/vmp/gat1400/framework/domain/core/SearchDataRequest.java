package com.genersoft.iot.vmp.gat1400.framework.domain.core;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchDataRequest extends PaginationRequest {

    @ApiModelProperty("视图库编号")
    private String serverId;
    @ApiModelProperty("视图库名称")
    private String serverName;
    @ApiModelProperty("检索关键字")
    private String keyword;
    @ApiModelProperty("开始时间")
    private String beginTime;
    @ApiModelProperty("结束时间")
    private String endTime;
}
