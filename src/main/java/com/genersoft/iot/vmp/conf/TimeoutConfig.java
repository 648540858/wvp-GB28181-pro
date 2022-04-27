package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeoutConfig {

    /**
     * 录像查询超时
     */
    @Value("${gb_record.query.timeout:90000}")
    private Long gbRecordQueryTimeout = 90000L;

    public Long getGbRecordQueryTimeout() {
        return gbRecordQueryTimeout;
    }
}
