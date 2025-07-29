package com.genersoft.iot.vmp.conf.ftpServer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 配置文件 user-settings 映射的配置信息
 */
@Component
@ConfigurationProperties(prefix = "ftp", ignoreInvalidFields = true)
@Order(0)
@Data
public class FtpSetting {

    private Boolean enable = Boolean.FALSE;

    private int port = 21;
    private String username = "admin";
    private String password = "admin";
    private String passivePorts = "10000-10500";
}
