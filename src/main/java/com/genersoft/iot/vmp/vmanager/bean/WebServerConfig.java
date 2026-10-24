package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * WEB服务(server)配置
 * <p>
 * 其中 port / ssl* 为当前生效配置文件中的值(可编辑), runtime* 为运行中实际生效的值(只读)
 */
@Setter
@Getter
@Schema(description = "WEB服务配置")
public class WebServerConfig {

    @Schema(description = "服务端口(配置文件中的值)")
    private Integer port;

    @Schema(description = "是否开启HTTPS(配置文件中的值)")
    private Boolean sslEnabled;

    @Schema(description = "PEM证书文件路径(配置文件中的值)")
    private String sslCertificate;

    @Schema(description = "私钥文件路径(配置文件中的值)")
    private String sslCertificatePrivateKey;

    @Schema(description = "运行中实际生效的服务端口(只读)")
    private Integer runtimePort;

    @Schema(description = "运行中是否已开启HTTPS(只读)")
    private Boolean runtimeSslEnabled;

    @Schema(description = "当前生效的配置文件ID")
    private String profileId;

    @Schema(description = "目标配置文件路径")
    private String filePath;

    @Schema(description = "目标配置文件是否可写")
    private boolean writable;

    @Schema(description = "保存后需要重启服务才能生效的字段")
    private List<String> restartRequiredFields;
}
