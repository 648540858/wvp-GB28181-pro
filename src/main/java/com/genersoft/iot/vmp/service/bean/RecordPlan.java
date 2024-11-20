package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "录制计划")
public class RecordPlan {

    @Schema(description = "计划数据库ID")
    private int id;

    @Schema(description = "计划名称")
    private String name;

    @Schema(description = "计划关联通道数量")
    private int channelCount;

    @Schema(description = "是否开启定时截图")
    private Boolean snap;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "计划内容")
    private List<RecordPlanItem> planItemList;
}
