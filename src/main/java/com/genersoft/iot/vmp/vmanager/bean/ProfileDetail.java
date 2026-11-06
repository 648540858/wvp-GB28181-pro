package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 配置文件详情
 * <p>
 * 用于系统设置-配置文件详情页的查看与在线编辑
 */
@Setter
@Getter
@Schema(description = "配置文件详情")
public class ProfileDetail {

    @Schema(description = "配置文件ID, 即 application-{id}.yml 中的 {id}")
    private String profileId;

    @Schema(description = "文件名, 如 application-dev.yml")
    private String fileName;

    @Schema(description = "配置文件路径")
    private String path;

    @Schema(description = "配置文件内容")
    private String content;

    @Schema(description = "内容SHA-256摘要, 保存时用于并发校验")
    private String contentHash;

    @Schema(description = "文件是否可写")
    private boolean writable;

    @Schema(description = "当前用户是否可编辑")
    private boolean editable;

    @Schema(description = "是否为当前生效的配置文件")
    private boolean active;

    @Schema(description = "是否为主配置文件 application.yml")
    private boolean mainConfig;

    @Schema(description = "文件修改时间, 格式 yyyy-MM-dd HH:mm:ss")
    private String updateTime;
}
