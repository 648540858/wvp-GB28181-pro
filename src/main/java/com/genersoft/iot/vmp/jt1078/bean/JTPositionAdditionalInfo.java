package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "位置附加信息")
public class JTPositionAdditionalInfo {

    @Schema(description = "里程, 单位为1/10km, 对应车上里程表读数")
    private int mileage;

    @Schema(description = "油量, 单位为1/10L, 对应车上油量表读数")
    private int oil;

    @Schema(description = "行驶记录功能获取的速度,单位为1/10km/h")
    private int speed;

    @Schema(description = "报警事件的 ID")
    private int alarmId;
    // TODO 暂不支持胎压

    @Schema(description = "车厢温度 ,单位为摄氏度")
    private int carriageTemperature;
    // TODO 暂不支持胎压

}
