package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * 业务分组
 */
@Data
@Schema(description = "业务分组")
public class Group implements Comparable<Group>{
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
     * 所属的业务分组国标编号
     */
    @Schema(description = "所属的业务分组国标编号")
    private String businessGroup;

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

    /**
     * 平台ID
     */
    @Schema(description = "平台ID")
    private Integer platformId;

    @Override
    public int compareTo(@NotNull Group region) {
        return Integer.compare(Integer.parseInt(this.deviceId), Integer.parseInt(region.getDeviceId()));
    }
}
