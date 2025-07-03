package com.genersoft.iot.vmp.gb28181.controller.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description="提交行政区划关联多个通道的参数")
public class ChannelToRegionParam {

    @Schema(description = "行政区划编号")
    private String civilCode;

    @Schema(description = "选择的通道， 和all参数二选一")
    private List<Integer> channelIds;

    @Schema(description = "所有通道， 和channelIds参数二选一")
    private Boolean all;

}
