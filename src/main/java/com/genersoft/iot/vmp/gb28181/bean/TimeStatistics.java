package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "时间统计信息")
public class TimeStatistics {

    @Schema(description = "时间")
    private String time;

    @Schema(description = "时间差")
    private Long timeDiff;
}
