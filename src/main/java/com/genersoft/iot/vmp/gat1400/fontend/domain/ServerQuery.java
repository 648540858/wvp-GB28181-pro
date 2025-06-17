package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.PaginationRequest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerQuery extends PaginationRequest {

    private String serverId;
    private String category;
    private String serverName;
    @ApiModelProperty("是否在线")
    private String isOnline;
    @ApiModelProperty("是否排除当前视图库")
    private boolean excludeSelf;
}
