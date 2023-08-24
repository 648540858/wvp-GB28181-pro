package com.genersoft.iot.vmp.service.bean;

/**
 * 业务分组数据
 */
public class BusinessGroup {

    /**
     * 数据库自增ID
     */
    private int commonBusinessGroupId;

    /**
     * 分组国标编号
     */
    private String commonBusinessGroupDeviceId;

    /**
     * 分组名称
     */
    private String commonBusinessGroupName;

    /**
     * 分组名称
     */
    private String commonBusinessGroupParentId;

    /**
     * 分组树的路径
     */
    private String commonBusinessGroupPath;

    /**
     * 创建时间
     */
    private String commonBusinessGroupCreateTime;

    /**
     * 更新时间
     */
    private String commonBusinessGroupUpdateTime;

    public int getCommonBusinessGroupId() {
        return commonBusinessGroupId;
    }

    public void setCommonBusinessGroupId(int commonBusinessGroupId) {
        this.commonBusinessGroupId = commonBusinessGroupId;
    }

    public String getCommonBusinessGroupDeviceId() {
        return commonBusinessGroupDeviceId;
    }

    public void setCommonBusinessGroupDeviceId(String commonBusinessGroupDeviceId) {
        this.commonBusinessGroupDeviceId = commonBusinessGroupDeviceId;
    }

    public String getCommonBusinessGroupName() {
        return commonBusinessGroupName;
    }

    public void setCommonBusinessGroupName(String commonBusinessGroupName) {
        this.commonBusinessGroupName = commonBusinessGroupName;
    }

    public String getCommonBusinessGroupPath() {
        return commonBusinessGroupPath;
    }

    public void setCommonBusinessGroupPath(String commonBusinessGroupPath) {
        this.commonBusinessGroupPath = commonBusinessGroupPath;
    }

    public String getCommonBusinessGroupParentId() {
        return commonBusinessGroupParentId;
    }

    public void setCommonBusinessGroupParentId(String commonBusinessGroupParentId) {
        this.commonBusinessGroupParentId = commonBusinessGroupParentId;
    }

    public String getCommonBusinessGroupCreateTime() {
        return commonBusinessGroupCreateTime;
    }

    public void setCommonBusinessGroupCreateTime(String commonBusinessGroupCreateTime) {
        this.commonBusinessGroupCreateTime = commonBusinessGroupCreateTime;
    }

    public String getCommonBusinessGroupUpdateTime() {
        return commonBusinessGroupUpdateTime;
    }

    public void setCommonBusinessGroupUpdateTime(String commonBusinessGroupUpdateTime) {
        this.commonBusinessGroupUpdateTime = commonBusinessGroupUpdateTime;
    }
}
