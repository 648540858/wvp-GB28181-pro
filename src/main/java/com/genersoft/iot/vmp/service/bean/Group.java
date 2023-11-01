package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分组数据
 */
@Schema(description = "分组数据")
public class Group {

    /**
     * 数据库自增ID
     */
    @Schema(description = "数据库自增ID")
    private int commonGroupId;

    /**
     * 分组国标编号
     */
    @Schema(description = "分组国标编号")
    private String commonGroupDeviceId;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称")
    private String commonGroupName;

    /**
     * 分组父ID
     */
    @Schema(description = "分组父ID")
    private String commonGroupParentId;

    /**
     * 分组的顶级节点ID，对应多个虚拟组织的业务分组ID
     */
    @Schema(description = "分组的顶级节点ID，对应多个虚拟组织的业务分组ID")
    private String commonGroupTopId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String commonGroupCreateTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String commonGroupUpdateTime;

    public static Group getInstance(String commonGroupDeviceId, String commonGroupName, String commonGroupParentId, String commonGroupTopId) {
        Group group = new Group();
        group.setCommonGroupDeviceId(commonGroupDeviceId);
        group.setCommonGroupName(commonGroupName);
        group.setCommonGroupParentId(commonGroupParentId);
        group.setCommonGroupTopId(commonGroupTopId);
        group.setCommonGroupCreateTime(DateUtil.getNow());
        group.setCommonGroupUpdateTime(DateUtil.getNow());
        return group;
    }

    public int getCommonGroupId() {
        return commonGroupId;
    }

    public void setCommonGroupId(int commonGroupId) {
        this.commonGroupId = commonGroupId;
    }

    public String getCommonGroupDeviceId() {
        return commonGroupDeviceId;
    }

    public void setCommonGroupDeviceId(String commonGroupDeviceId) {
        this.commonGroupDeviceId = commonGroupDeviceId;
    }

    public String getCommonGroupName() {
        return commonGroupName;
    }

    public void setCommonGroupName(String commonGroupName) {
        this.commonGroupName = commonGroupName;
    }

    public String getCommonGroupParentId() {
        return commonGroupParentId;
    }

    public void setCommonGroupParentId(String commonGroupParentId) {
        this.commonGroupParentId = commonGroupParentId;
    }

    public String getCommonGroupCreateTime() {
        return commonGroupCreateTime;
    }

    public void setCommonGroupCreateTime(String commonGroupCreateTime) {
        this.commonGroupCreateTime = commonGroupCreateTime;
    }

    public String getCommonGroupUpdateTime() {
        return commonGroupUpdateTime;
    }

    public void setCommonGroupUpdateTime(String commonGroupUpdateTime) {
        this.commonGroupUpdateTime = commonGroupUpdateTime;
    }

    public String getCommonGroupTopId() {
        return commonGroupTopId;
    }

    public void setCommonGroupTopId(String commonGroupTopId) {
        this.commonGroupTopId = commonGroupTopId;
    }
}
