package com.genersoft.iot.vmp.gb28181.controller.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DrawThinParam {
    private Map<Integer, Double> zoomParam;
    private Extent extent;

    /**
     * 地理坐标系， WGS84/GCJ02， 用来标识 extent 参数的坐标系
     */
    private String geoCoordSys;
}
