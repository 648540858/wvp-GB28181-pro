package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "设置区域参数")
public class SetAreaParam {

    @Schema(description = "设备")
    private String phoneNumber;

    @Schema(description = "圆形区域项")
    private List<JTCircleArea> circleAreaList;

    @Schema(description = "矩形区域项")
    private List<JTRectangleArea> rectangleAreas;

    @Schema(description = "多边形区域")
    private JTPolygonArea polygonArea;

    @Schema(description = "路线")
    private JTRoute route;


    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<JTCircleArea> getCircleAreaList() {
        return circleAreaList;
    }

    public void setCircleAreaList(List<JTCircleArea> circleAreaList) {
        this.circleAreaList = circleAreaList;
    }

    public List<JTRectangleArea> getRectangleAreas() {
        return rectangleAreas;
    }

    public void setRectangleAreas(List<JTRectangleArea> rectangleAreas) {
        this.rectangleAreas = rectangleAreas;
    }

    public JTPolygonArea getPolygonArea() {
        return polygonArea;
    }

    public void setPolygonArea(JTPolygonArea polygonArea) {
        this.polygonArea = polygonArea;
    }

    public JTRoute getRoute() {
        return route;
    }

    public void setRoute(JTRoute route) {
        this.route = route;
    }

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
