package com.genersoft.iot.vmp.streamPush.bean;

import com.alibaba.excel.annotation.ExcelProperty;
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
    private String gbId;

    @ExcelProperty("平台ID")
    private String platformId;

    @ExcelProperty("目录ID")
    private String catalogId;

    @ExcelProperty("在线状态")
    private boolean status;
}
