package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SystemTimeObject;

import lombok.Data;

@Data
public class SystemTimeResponse {

    @JsonProperty("SystemTimeObject")
    private SystemTimeObject systemTimeObject;

    public SystemTimeResponse() {}

    public SystemTimeResponse(String deviceId) {
        this.systemTimeObject = new SystemTimeObject(deviceId);
    }
}
