package com.genersoft.iot.vmp.vmanager.gb28181.gbStream.bean;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "国标关联参数")
public class GbStreamParam {

    @Schema(description = "平台ID")
    private String platformId;

    @Schema(description = "目录ID")
    private String catalogId;

    @Schema(description = "关联所有通道")
    private boolean all;

    @Schema(description = "流国标信息列表")
    private List<GbStream> gbStreams;

    public String getPlatformId() {
        return platformId;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<GbStream> getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(List<GbStream> gbStreams) {
        this.gbStreams = gbStreams;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }
}
