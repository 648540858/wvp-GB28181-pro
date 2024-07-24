package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * 区域
 */
@Data
@Schema(description = "区域")
public class Region implements Comparable<Region>{
    /**
     * 数据库自增ID
     */
    @Schema(description = "数据库自增ID")
    private int id;

    /**
     * 区域国标编号
     */
    @Schema(description = "区域国标编号")
    private String deviceId;

    /**
     * 区域名称
     */
    @Schema(description = "区域名称")
    private String name;

    /**
     * 父区域国标ID
     */
    @Schema(description = "父区域国标ID")
    private String parentDeviceId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String updateTime;

    public static Region getInstance(String commonRegionDeviceId, String commonRegionName, String commonRegionParentId) {
        Region region = new Region();
        region.setDeviceId(commonRegionDeviceId);
        region.setName(commonRegionName);
        region.setParentDeviceId(commonRegionParentId);
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        return region;
    }

    public static Region getInstance(CivilCodePo civilCodePo) {
        Region region = new Region();
        region.setName(civilCodePo.getName());
        region.setDeviceId(civilCodePo.getCode());
        if (civilCodePo.getCode().length() > 2) {
            region.setParentDeviceId(civilCodePo.getParentCode());
        }
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        return region;
    }

    @Override
    public int compareTo(@NotNull Region region) {
        return Integer.compare(Integer.parseInt(this.deviceId), Integer.parseInt(region.getDeviceId()));
    }
}
