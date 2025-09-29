package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "坐标")
public class Point {

    private double lng;
    private double lat;

}
