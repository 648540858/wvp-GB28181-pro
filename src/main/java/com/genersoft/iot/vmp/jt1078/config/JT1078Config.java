package com.genersoft.iot.vmp.jt1078.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "jt1078", ignoreInvalidFields = true)
@Order(3)
public class JT1078Config {

    private Integer port;

    private String password;

    private Boolean record = false;

    /**
     * IDLE状态超时时间，单位：秒，默认0表示不启用，启用后当连接进入IDLE状态超过该时间时将被断开连接
       连接进入IDLE状态的条件是：在readerIdleTime时间内没有收到任何数据包，并且在writerIdleTime时间内没有发送任何数据包
     */
    private Integer readerIdleTime = 0;
}
