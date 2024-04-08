package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "位置信息")
public class JTPositionInfo {

    /**
     * 位置基本信息
     */
    @Schema(description = "位置基本信息")
    private JTPositionBaseInfo base;

    /**
     * 位置基本信息
     */
    @Schema(description = "位置附加信息")
    private JTPositionAdditionalInfo additional;

    public JTPositionBaseInfo getBase() {
        return base;
    }

    public void setBase(JTPositionBaseInfo base) {
        this.base = base;
    }

    public JTPositionAdditionalInfo getAdditional() {
        return additional;
    }

    public void setAdditional(JTPositionAdditionalInfo additional) {
        this.additional = additional;
    }
}
