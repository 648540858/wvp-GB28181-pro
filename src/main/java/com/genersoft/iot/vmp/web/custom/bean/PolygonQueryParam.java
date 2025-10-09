package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "多边形检索摄像头参数")
public class PolygonQueryParam {

    @Schema(description = "多边形位置，格式： [{'lng':116.32, 'lat': 39: 39.2}, {'lng':115.32, 'lat': 39: 38.2}, {'lng':125.32, 'lat': 39: 38.2}]")
    private List<Point> position;

    @Schema(description = "地图级别")
    private Integer level;

    @Schema(description = "分组别名")
    private String groupAlias;

    @Schema(description = "坐标系类型：WGS84,GCJ02、BD09")
    private String geoCoordSys;
}
