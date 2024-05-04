package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTCircleArea;
import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import com.genersoft.iot.vmp.jt1078.bean.JTPolygonArea;
import com.genersoft.iot.vmp.jt1078.bean.JTRectangleArea;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "设置区域")
public class SetAreaParam {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "圆形区域项")
    private List<JTCircleArea> circleAreaList;

    @Schema(description = "矩形区域项")
    private List<JTRectangleArea> rectangleAreas;

    @Schema(description = "多边形区域")
    private JTPolygonArea polygonArea;


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    @Override
    public String toString() {
        return "SetAreaParam{" +
                "deviceId='" + deviceId + '\'' +
                ", circleAreaList=" + circleAreaList +
                ", rectangleAreas=" + rectangleAreas +
                ", polygonArea=" + polygonArea +
                '}';
    }
}
