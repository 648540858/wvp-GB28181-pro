package com.genersoft.iot.vmp.vmanager.bean;

import lombok.Data;

@Data
public class MapConfig {

    private String name;
    private String coordinateSystem;
    private String tilesUrl;
    private Integer tileSize;
    private Integer zoom;
    private Double[] center;
    private Integer maxZoom;
    private Integer minZoom;
}
