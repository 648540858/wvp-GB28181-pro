package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 部标(JT/T 1078)服务配置
 * <p>
 * 其中 enable/port 等为当前生效配置文件中的值(可编辑), runtime* 为运行中实际生效的值(只读)
 */
@Setter
@Getter
@Schema(description = "部标JT1078服务配置")
public class JtServerConfig {

    @Schema(description = "是否开启FTP服务(配置文件中的值)")
    private Boolean ftpEnable;

    @Schema(description = "FTP服务端口(配置文件中的值)")
    private Integer ftpPort;

    @Schema(description = "FTP被动模式端口范围(配置文件中的值)")
    private String ftpPassivePorts;

    @Schema(description = "是否开启JT1078服务(配置文件中的值)")
    private Boolean jt1078Enable;

    @Schema(description = "JT1078设备接入端口(配置文件中的值)")
    private Integer jt1078Port;

    @Schema(description = "JT1078设备鉴权密码(配置文件中的值)")
    private String jt1078Password;

    @Schema(description = "是否录像(配置文件中的值)")
    private Boolean jt1078Record;

    @Schema(description = "连接IDLE状态超时时间(秒), 0表示不启用(配置文件中的值)")
    private Integer jt1078ReaderIdleTime;

    @Schema(description = "运行中FTP服务是否已开启(只读)")
    private Boolean runtimeFtpEnable;

    @Schema(description = "运行中JT1078服务是否已开启(只读)")
    private Boolean runtimeJt1078Enable;

    @Schema(description = "当前生效的配置文件ID")
    private String profileId;

    @Schema(description = "目标配置文件路径")
    private String filePath;

    @Schema(description = "目标配置文件是否可写")
    private boolean writable;

    @Schema(description = "保存后需要重启服务才能生效的字段")
    private List<String> restartRequiredFields;
}
