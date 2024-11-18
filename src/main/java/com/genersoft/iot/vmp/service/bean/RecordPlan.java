package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "录制计划")
public class RecordPlan {

    @Schema(description = "计划数据库ID")
    private int id;

    @Schema(description = "计划关联的通道ID")
    private Integer channelId;

    @Schema(description = "计划开始时间")
    private Long startTime;

    @Schema(description = "计划结束时间")
    private Long stopTime;

    @Schema(description = "计划周几执行")
    private Integer weekDay;

    @Schema(description = "是否开启定时截图")
    private Boolean snap;
}
