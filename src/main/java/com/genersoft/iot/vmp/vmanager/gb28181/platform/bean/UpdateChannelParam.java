package com.genersoft.iot.vmp.vmanager.gb28181.platform.bean;

import com.genersoft.iot.vmp.common.CommonGbChannel;
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

    @Schema(description = "待关联的通道ID")
    private List<Integer> commonGbChannelIds;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<Integer> getCommonGbChannelIds() {
        return commonGbChannelIds;
    }

    public void setCommonGbChannelIds(List<Integer> commonGbChannelIds) {
        this.commonGbChannelIds = commonGbChannelIds;
    }
}
