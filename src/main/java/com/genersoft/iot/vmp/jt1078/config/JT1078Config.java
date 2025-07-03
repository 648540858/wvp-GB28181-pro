package com.genersoft.iot.vmp.jt1078.config;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
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
}
