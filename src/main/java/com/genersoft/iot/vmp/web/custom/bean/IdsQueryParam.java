package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "根据多个ID获取摄像头列表")
public class IdsQueryParam {

    @Schema(description = "通道编号列表")
    private List<ChannelParam> deviceIds;

    @Schema(description = "坐标系类型：WGS84,GCJ02、BD09")
    private String geoCoordSys;
}
