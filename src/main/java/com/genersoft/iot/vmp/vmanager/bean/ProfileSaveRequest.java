package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 保存配置文件内容的请求
 */
@Setter
@Getter
@Schema(description = "保存配置文件内容")
public class ProfileSaveRequest {

    @Schema(description = "配置文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String profileId;

    @Schema(description = "配置文件内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "读取时的内容SHA-256摘要, 用于并发校验")
    private String contentHash;
}
