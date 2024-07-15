package com.genersoft.iot.vmp.streamPush.bean;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StreamPushExcelDto {

    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("应用名")
    private String app;

    @ExcelProperty("流ID")
    private String stream;

    @ExcelProperty("国标ID")
    private String gbDeviceId;

    @ExcelProperty("在线状态")
    private boolean status;

    @Schema(description = "经度 WGS-84坐标系")
    private Double longitude;

    @Schema(description = "纬度 WGS-84坐标系")
    private Double latitude;
}
