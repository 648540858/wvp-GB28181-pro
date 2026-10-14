package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局策略-平台策略(user-settings)配置的保存结果
 * <p>
 * 保存时部分字段会被就地热应用立即生效, 另一部分字段在启动阶段已被固化,
 * 必须重启服务才能生效, 由 {@link #restartFields} 给出本次变更且需要重启的字段。
 */
@Setter
@Getter
@Schema(description = "平台策略配置保存结果")
public class UserSettingSaveResult {

    @Schema(description = "被修改的配置文件路径")
    private String filePath;

    @Schema(description = "是否需要重启服务才能生效")
    private boolean restartRequired;

    @Schema(description = "本次变更且需要重启才能生效的字段")
    private List<String> restartFields = new ArrayList<>();
}
