package com.genersoft.iot.vmp.vmanager.recordPlan.bean;

import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "录制计划-添加/编辑参数")
public class RecordPlanParam {

    @Schema(description = "关联的通道ID")
    private Integer channelId;

    @Schema(description = "关联的设备ID，会为设备下的所有通道关联此录制计划，channelId存在是此项不生效，")
    private Integer deviceDbId;

    @Schema(description = "录制计划ID, ID为空是删除关联的计划")
    private Integer planId;
}
