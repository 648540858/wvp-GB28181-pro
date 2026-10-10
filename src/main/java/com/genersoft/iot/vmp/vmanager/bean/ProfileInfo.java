package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 配置文件信息
 */
@Setter
@Getter
@Schema(description = "配置文件信息")
public class ProfileInfo {

    @Schema(description = "配置文件ID, 即 application-{id}.yml 中的 {id}")
    private String id;

    @Schema(description = "配置文件名称, 即文件名")
    private String name;

    @Schema(description = "是否为当前正在使用的配置文件")
    private boolean active;

    @Schema(description = "文件创建时间, 格式 yyyy-MM-dd HH:mm:ss")
    private String createTime;

    @Schema(description = "文件修改时间, 格式 yyyy-MM-dd HH:mm:ss")
    private String updateTime;

    @Schema(description = "文件完整路径")
    private String path;

    @Schema(description = "是否可切换(配置文件可写)")
    private boolean writable;

    public ProfileInfo() {
    }

    public ProfileInfo(String id, String name, boolean active, String createTime, String updateTime, String path) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.path = path;
    }
}
