package com.genersoft.iot.vmp.vmanager.gb28181.platform.bean;

import java.util.List;

public class UpdateChannelParam {
    private String platformId;
    private String catalogId;
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
}
