package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 删除配置文件的请求
 */
@Setter
@Getter
@Schema(description = "删除配置文件")
public class ProfileDeleteRequest {

    @Schema(description = "配置文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String profileId;
}
