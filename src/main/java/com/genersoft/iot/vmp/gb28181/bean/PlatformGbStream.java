package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;

public class PlatformGbStream {

    @Schema(description = "ID")
    private int gbStreamId;

    @Schema(description = "平台ID")
    private String platformId;

    @Schema(description = "目录ID")
    private String catalogId;

    public Integer getGbStreamId() {
        return gbStreamId;
    }

    public void setGbStreamId(Integer gbStreamId) {
        this.gbStreamId = gbStreamId;
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
}
