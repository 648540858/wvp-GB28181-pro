package com.genersoft.iot.vmp.vmanager.gb28181.platform.bean;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 通道关联参数
 * @author lin
 */
@Schema(description = "通道关联参数")
public class UpdateChannelParam {

    @Schema(description = "上级平台的国标编号")
    private String platformId;

    @Schema(description = "目录的国标编号")
    private String catalogId;

    @Schema(description = "处理所有通道")
    private boolean all;

    @Schema(description = "")
    private List<ChannelReduce> channelReduces;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<ChannelReduce> getChannelReduces() {
        return channelReduces;
    }

    public void setChannelReduces(List<ChannelReduce> channelReduces) {
        this.channelReduces = channelReduces;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }
}
