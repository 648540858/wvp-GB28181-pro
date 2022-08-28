package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 国标级联-目录
 * @author lin
 */
@Schema(description = "目录信息")
public class PlatformCatalog {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "平台ID")
    private String platformId;

    @Schema(description = "父级目录ID")
    private String parentId;

    @Schema(description = "行政区划")
    private String civilCode;

    @Schema(description = "目录分组")
    private String businessGroupId;

    /**
     * 子节点数
     */
    @Schema(description = "子节点数")
    private int childrenCount;

    /**
     * 0 目录, 1 国标通道, 2 直播流
     */
    @Schema(description = "类型：0 目录, 1 国标通道, 2 直播流")
    private int type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setTypeForCatalog() {
        this.type = 0;
    }

    public void setTypeForGb() {
        this.type = 1;
    }

    public void setTypeForStream() {
        this.type = 2;
    }

    public String getCivilCode() {
        return civilCode;
    }

    public void setCivilCode(String civilCode) {
        this.civilCode = civilCode;
    }

    public String getBusinessGroupId() {
        return businessGroupId;
    }

    public void setBusinessGroupId(String businessGroupId) {
        this.businessGroupId = businessGroupId;
    }
}
