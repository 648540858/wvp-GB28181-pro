package com.genersoft.iot.vmp.vmanager.channel.bean;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

@Schema(description = "共享通道参数")
public class ShareCommonGbChannelParam {

    @Schema(description = "通道ID列表")
    private Set<Integer> channelIdList;


    @Schema(description = "平台ID")
    private Integer platformId;

    public Set<Integer> getChannelIdList() {
        return channelIdList;
    }

    public void setChannelIdList(Set<Integer> channelIdList) {
        this.channelIdList = channelIdList;
    }

    public Integer getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Integer platformId) {
        this.platformId = platformId;
    }
}
