package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 全局策略-流媒体(media)配置
 * <p>
 * 其中 id/ip 等为当前生效配置文件中的值(可编辑), runtime* 为运行中实际生效的值(只读)
 */
@Setter
@Getter
@Schema(description = "流媒体(media)服务配置")
public class MediaServerConfig {

    @Schema(description = "流媒体服务器唯一ID(配置文件中的值)")
    private String id;

    @Schema(description = "流媒体服务器内网IP(配置文件中的值)")
    private String ip;

    @Schema(description = "流媒体服务器公网IP(配置文件中的值)")
    private String wanIp;

    @Schema(description = "返回流地址时的IP(配置文件中的值)")
    private String streamIp;

    @Schema(description = "国标信令中使用的IP(配置文件中的值)")
    private String sdpIp;

    @Schema(description = "流媒体服务器访问WVP所使用的IP(配置文件中的值)")
    private String hookIp;

    @Schema(description = "流媒体服务器HTTP端口(配置文件中的值)")
    private Integer httpPort;

    @Schema(description = "流媒体服务器鉴权密钥(配置文件中的值)")
    private String secret;

    @Schema(description = "是否自动配置流媒体服务器(配置文件中的值)")
    private Boolean autoConfig;

    @Schema(description = "多端口模式开关(配置文件中的值)")
    private Boolean rtpEnable;

    @Schema(description = "收流端口范围(配置文件中的值)")
    private String rtpPortRange;

    @Schema(description = "发流端口范围(配置文件中的值)")
    private String rtpSendPortRange;

    @Schema(description = "录像辅助服务端口, 0表示不使用(配置文件中的值)")
    private Integer recordAssistPort;

    @Schema(description = "录像保存时长(天)(配置文件中的值)")
    private Integer recordDay;

    @Schema(description = "录像保存路径(配置文件中的值)")
    private String recordPath;

    @Schema(description = "流媒体服务类型(配置文件中的值)")
    private String type;

    @Schema(description = "运行中实际生效的流媒体ID(只读)")
    private String runtimeId;

    @Schema(description = "运行中实际生效的流媒体IP(只读)")
    private String runtimeIp;

    @Schema(description = "运行中实际生效的HTTP端口(只读)")
    private Integer runtimeHttpPort;

    @Schema(description = "当前生效的配置文件ID")
    private String profileId;

    @Schema(description = "目标配置文件路径")
    private String filePath;

    @Schema(description = "目标配置文件是否可写")
    private boolean writable;
}
