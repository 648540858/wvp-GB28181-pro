package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "多边形检索摄像头参数")
public class PolygonQueryParam {

    private List<Point> position;
    private Integer level;
    private String groupAlias;
    private String topGroupAlias;
    private String geoCoordSys;
}
