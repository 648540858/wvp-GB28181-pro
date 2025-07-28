package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
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

    public void setBase(JTPositionBaseInfo base) {
        this.base = base;
    }

    public void setAdditional(JTPositionAdditionalInfo additional) {
        this.additional = additional;
    }
}
