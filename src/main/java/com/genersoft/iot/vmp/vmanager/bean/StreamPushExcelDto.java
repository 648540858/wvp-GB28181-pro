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

    @ExcelProperty("平台ID")
    private String platformId;

    @ExcelProperty("目录ID")
    private String catalogId;

    @ExcelProperty("在线状态")
    private boolean status;

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


    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public boolean isStatus() {
        return status;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
