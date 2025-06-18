package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import lombok.Data;

@Data
public class SystemTimeObject {

    @JsonProperty("VIIDServerID")
    private String viidServerId;
    @JsonProperty("TimeMode")
    private String timeMode;
    @JsonProperty("LocalTime")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date localTime;
    @JsonProperty("TimeZone")
    private String timezone;

    public SystemTimeObject() {}

    public SystemTimeObject(String deviceId) {
        this.viidServerId = deviceId;
        this.timeMode = "1";
        this.localTime = new Date();
        this.timezone = null;
    }
}
