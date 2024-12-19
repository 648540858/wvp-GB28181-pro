package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "录制计划项")
public class RecordPlanItem {

    @Schema(description = "计划项数据库ID")
    private int id;

    @Schema(description = "计划开始时间的序号， 从0点开始，每半个小时增加1")
    private Integer start;

    @Schema(description = "计划结束时间的序号， 从0点开始，每半个小时增加1")
    private Integer stop;

    @Schema(description = "计划周几执行")
    private Integer weekDay;

    @Schema(description = "所属计划ID")
    private Integer planId;

}
