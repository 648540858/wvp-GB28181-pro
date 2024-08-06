package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 业务分组
 */
@Data
@Schema(description = "业务分组树")
public class GroupTree {

    /**
     * 数据库Id
     */
    @Schema(description = "数据库Id")
    private int dbId;

    /**
     * 区域国标编号
     */
    @Schema(description = "区域国标编号")
    private String id;

    /**
     * 区域名称
     */
    @Schema(description = "区域名称")
    private String label;

    /**
     * 父区域国标ID
     */
    @Schema(description = "父区域国标ID")
    private String parentDeviceId;

    /**
     * 业务分组ID
     */
    @Schema(description = "父区域国标ID")
    private String businessGroup;

    @Schema(description = "是否有子节点")
    private boolean isLeaf;

    @Schema(description = "类型, 行政区划:0 摄像头: 1")
    private int type;

    public static GroupTree getInstance(CommonGBChannel channel) {
        GroupTree regionTree = new GroupTree();
        regionTree.setId(channel.getGbDeviceId());
        regionTree.setLabel(channel.getGbName());
        regionTree.setParentDeviceId(channel.getGbCivilCode());
        regionTree.setType(1);
        regionTree.setLeaf(true);
        return regionTree;
    }
}
