package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 国标(GB28181) SIP 服务配置
 * <p>
 * 其中 ip/showIp/port/domain/id/password 等为当前生效配置文件中的值(可编辑),
 * runtime* 为运行中实际生效的值(只读)
 */
@Setter
@Getter
@Schema(description = "国标SIP服务配置")
public class SipServerConfig {

    @Schema(description = "监听网卡IP(配置文件中的值)")
    private String ip;

    @Schema(description = "对外展示IP(配置文件中的值)")
    private String showIp;

    @Schema(description = "SIP服务监听端口(配置文件中的值)")
    private Integer port;

    @Schema(description = "国标域(配置文件中的值)")
    private String domain;

    @Schema(description = "国标编号(配置文件中的值)")
    private String id;

    @Schema(description = "公共认证密码(配置文件中的值)")
    private String password;

    @Schema(description = "国标级联注册失败再次发起注册的时间间隔(秒)")
    private Integer registerTimeInterval;

    @Schema(description = "云台控制速度")
    private Integer ptzSpeed;

    @Schema(description = "是否存储alarm信息")
    private Boolean alarm;

    @Schema(description = "命令发送等待回复的超时时间(毫秒)")
    private Long timeout;

    @Schema(description = "运行中实际生效的服务端口(只读)")
    private Integer runtimePort;

    @Schema(description = "运行中实际监听的网卡IP(只读)")
    private String runtimeIp;

    @Schema(description = "运行中实际对外展示IP(只读)")
    private String runtimeShowIp;

    @Schema(description = "运行中实际监听的网卡IP列表(只读)")
    private List<String> runtimeMonitorIps;

    @Schema(description = "当前生效的配置文件ID")
    private String profileId;

    @Schema(description = "目标配置文件路径")
    private String filePath;

    @Schema(description = "目标配置文件是否可写")
    private boolean writable;
}
