package com.genersoft.iot.vmp.conf.ftpServer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 配置文件 user-settings 映射的配置信息
 */
@Component
@ConfigurationProperties(prefix = "ftp", ignoreInvalidFields = true)
@Order(0)
public class FtpSetting {

    private Boolean enable = Boolean.FALSE;

    private String ip;
    private int port = 21;
    private String username = "admin";
    private String password = "admin";

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
