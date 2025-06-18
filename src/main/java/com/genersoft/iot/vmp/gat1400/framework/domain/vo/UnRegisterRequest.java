package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.DeviceIdObject;

import lombok.Data;

@Data
public class UnRegisterRequest {

    @JsonProperty("UnRegisterObject")
    private DeviceIdObject unRegisterObject;
}
