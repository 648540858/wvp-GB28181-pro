package com.genersoft.iot.vmp.gb28181.bean;

public class PlatformCatalog {
    private String id;
    private String name;
    private String platformId;
    private String parentId;
    private int childrenCount; // 子节点数
    private int type; // 0 目录, 1 国标通道, 2 直播流

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

}
