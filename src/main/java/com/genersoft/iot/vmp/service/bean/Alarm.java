package com.genersoft.iot.vmp.service.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "报警信息")
public class Alarm {

    @Schema(description = "数据库id")
    private Long id;

    @Schema(description = "关联通道的数据库id")
    private int channelId;

    @Schema(description = "报警描述")
    private String description;

    @Schema(description = "报警快照路径")
    private String snapPath;

    @Schema(description = "报警录像路径")
    private String recordPath;

    @Schema(description = "报警附带的经度")
    private String longitude;

    @Schema(description = "报警附带的纬度")
    private String latitude;

    @Schema(description = "报警类别")
    private AlarmType alarmType;

    @Schema(description = "报警时间")
    private Long alarmTime;



}
