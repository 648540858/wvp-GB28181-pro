package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;

import lombok.Data;

@Data
public class VIIDBaseResponse {

    @JsonProperty("ResponseStatusObject")
    private ResponseStatusObject responseStatusObject;

    public VIIDBaseResponse() {}

    public VIIDBaseResponse(ResponseStatusObject responseStatusObject) {
        this.responseStatusObject = responseStatusObject;
    }
}
