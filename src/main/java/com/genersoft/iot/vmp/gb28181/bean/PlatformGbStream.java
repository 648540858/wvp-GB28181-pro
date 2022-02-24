package com.genersoft.iot.vmp.gb28181.bean;

public class PlatformGbStream {
    private Integer gbStreamId;
    private String platformId;
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
