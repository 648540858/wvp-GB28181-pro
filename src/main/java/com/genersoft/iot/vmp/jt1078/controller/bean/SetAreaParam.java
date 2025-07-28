package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Schema(description = "设置区域参数")
public class SetAreaParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;

    @Schema(description = "圆形区域项")
    private List<JTCircleArea> circleAreaList;

    @Schema(description = "矩形区域项")
    private List<JTRectangleArea> rectangleAreas;

    @Schema(description = "多边形区域")
    private JTPolygonArea polygonArea;

    @Schema(description = "路线")
    private JTRoute route;


    @Override
    public String toString() {
        return "SetAreaParam{" +
                "设备手机号='" + phoneNumber + '\'' +
                ", circleAreaList=" + circleAreaList +
                ", rectangleAreas=" + rectangleAreas +
                ", polygonArea=" + polygonArea +
                ", route=" + route +
                '}';
    }
}
