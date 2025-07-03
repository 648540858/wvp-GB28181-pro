package com.genersoft.iot.vmp.common;

import lombok.Data;

/**
 * 统计信息
 */
@Data
public class StatisticsInfo {

    private long id;

    /**
     * ID
     */
    private String deviceId;

    /**
     * 分支
     */
    private String branch;

    /**
     * git提交版本ID
     */
    private String gitCommitId;

    /**
     * git地址
     */
    private String gitUrl;

    /**
     * 构建版本
     */
    private String version;

    /**
     * 操作系统名称
     */
    private String osName;

    /**
     * 是否是docker环境
     */
    private Boolean docker;

    /**
     * 架构
     */
    private String arch;

    /**
     * jdk版本
     */
    private String jdkVersion;

    /**
     * redis版本
     */
    private String redisVersion;

    /**
     * sql数据库版本
     */
    private String sqlVersion;

    /**
     * sql数据库类型， mysql/postgresql/金仓等
     */
    private String sqlType;

    /**
     * 创建时间
     */
    private String time;

    @Override
    public String toString() {
        return "StatisticsInfo{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", branch='" + branch + '\'' +
                ", gitCommitId='" + gitCommitId + '\'' +
                ", gitUrl='" + gitUrl + '\'' +
                ", version='" + version + '\'' +
                ", osName='" + osName + '\'' +
                ", docker=" + docker +
                ", arch='" + arch + '\'' +
                ", jdkVersion='" + jdkVersion + '\'' +
                ", redisVersion='" + redisVersion + '\'' +
                ", sqlVersion='" + sqlVersion + '\'' +
                ", sqlType='" + sqlType + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
