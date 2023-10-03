package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.utils.DateUtil;

/**
 * 区域
 */
public class Region {
    /**
     * 数据库自增ID
     */
    private int commonRegionId;

    /**
     * 区域国标编号
     */
    private String commonRegionDeviceId;

    /**
     * 区域名称
     */
    private String commonRegionName;

    /**
     * 父区域国标ID
     */
    private String commonRegionParentId;

    /**
     * 创建时间
     */
    private String commonRegionCreateTime;

    /**
     * 更新时间
     */
    private String commonRegionUpdateTime;

    public static Region getInstance(String commonRegionDeviceId, String commonRegionName, String commonRegionParentId) {
        Region region = new Region();
        region.setCommonRegionDeviceId(commonRegionDeviceId);
        region.setCommonRegionName(commonRegionName);
        region.setCommonRegionParentId(commonRegionParentId);
        region.setCommonRegionCreateTime(DateUtil.getNow());
        region.setCommonRegionUpdateTime(DateUtil.getNow());
        return region;
    }

    public int getCommonRegionId() {
        return commonRegionId;
    }

    public void setCommonRegionId(int commonRegionId) {
        this.commonRegionId = commonRegionId;
    }

    public String getCommonRegionDeviceId() {
        return commonRegionDeviceId;
    }

    public void setCommonRegionDeviceId(String commonRegionDeviceId) {
        this.commonRegionDeviceId = commonRegionDeviceId;
    }

    public String getCommonRegionName() {
        return commonRegionName;
    }

    public void setCommonRegionName(String commonRegionName) {
        this.commonRegionName = commonRegionName;
    }

    public String getCommonRegionParentId() {
        return commonRegionParentId;
    }

    public void setCommonRegionParentId(String commonRegionParentId) {
        this.commonRegionParentId = commonRegionParentId;
    }

    public String getCommonRegionCreateTime() {
        return commonRegionCreateTime;
    }

    public void setCommonRegionCreateTime(String commonRegionCreateTime) {
        this.commonRegionCreateTime = commonRegionCreateTime;
    }

    public String getCommonRegionUpdateTime() {
        return commonRegionUpdateTime;
    }

    public void setCommonRegionUpdateTime(String commonRegionUpdateTime) {
        this.commonRegionUpdateTime = commonRegionUpdateTime;
    }
}
