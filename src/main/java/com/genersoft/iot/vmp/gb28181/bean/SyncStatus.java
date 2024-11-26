package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 摄像机同步状态
 * @author lin
 */
@Data
@Schema(description = "摄像机同步状态")
public class SyncStatus {

    @Schema(description = "总数")
    private Integer total;

    @Schema(description = "当前更新多少")
    private Integer current;

    @Schema(description = "错误描述")
    private String errorMsg;

    @Schema(description = "是否同步中")
    private Boolean syncIng;

}
