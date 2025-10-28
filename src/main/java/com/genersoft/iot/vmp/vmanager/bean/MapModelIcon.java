package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MapModelIcon {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "别名")
    private String alias;

    @Schema(description = "路径")
    private String path;


    public static MapModelIcon getInstance(String name, String alias, String path) {
        MapModelIcon mapModelIcon = new MapModelIcon();
        mapModelIcon.setAlias(alias);
        mapModelIcon.setName(name);
        mapModelIcon.setPath(path);
        return mapModelIcon;
    }
}
