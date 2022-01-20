package com.genersoft.iot.vmp.vmanager.bean;

import com.alibaba.excel.annotation.ExcelProperty;

public class StreamPushExcelDto {

    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("应用名")
    private String app;

    @ExcelProperty("流ID")
    private String stream;

    @ExcelProperty("国标ID")
    private String gbId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }
}
