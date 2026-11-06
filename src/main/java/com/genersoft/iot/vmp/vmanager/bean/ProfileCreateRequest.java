package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 新建配置文件的请求
 */
@Setter
@Getter
@Schema(description = "新建配置文件")
public class ProfileCreateRequest {

    @Schema(description = "配置文件ID, 将生成 application-{id}.yml", requiredMode = Schema.RequiredMode.REQUIRED)
    private String profileId;

    @Schema(description = "初始内容, 为空时创建空白文件")
    private String content;
}
