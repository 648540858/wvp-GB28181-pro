package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 区域
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "区域树")
public class RegionTree extends Region {

    @Schema(description = "树节点ID")
    private String treeId;

    @Schema(description = "是否有子节点")
    private boolean isLeaf;

    @Schema(description = "类型, 行政区划:0 摄像头: 1")
    private int type;

    @Schema(description = "在线状态")
    private String status;
}
