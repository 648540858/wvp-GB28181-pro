package com.genersoft.iot.vmp.gb28181.controller.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "通道关联参数")
public class UpdateChannelParam {

    @Schema(description = "上级平台的数据库ID")
    private Integer platformId;


    @Schema(description = "关联所有通道")
    private boolean all;

    @Schema(description = "待关联的通道ID")
    List<Integer> channelIds;

    @Schema(description = "待关联的设备ID")
    List<Integer> deviceIds;
}
