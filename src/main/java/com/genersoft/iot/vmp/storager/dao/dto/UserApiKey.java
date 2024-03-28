package com.genersoft.iot.vmp.storager.dao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 用户信息
 */
@Schema(description = "用户ApiKey信息")
public class UserApiKey implements Serializable {

    /**
     * Id
     */
    @Schema(description = "Id")
    private int id;

    /**
     * 用户Id
     */
    @Schema(description = "用户Id")
    private int userId;

    /**
     * 应用名
     */
    @Schema(description = "应用名")
    private String app;

    /**
     * ApiKey
     */
    @Schema(description = "ApiKey")
    private String apiKey;

    /**
     * 过期时间（null=永不过期）
     */
    @Schema(description = "过期时间（null=永不过期）")
    private String expiredAt;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息")
    private String remark;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private boolean enable;

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
     * 用户名
     */
    private String username;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(String expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
