package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.DeviceIdObject;

import lombok.Data;

@Data
public class RegisterRequest {

    @JsonProperty("RegisterObject")
    private DeviceIdObject registerObject;

    public DeviceIdObject getRegisterObject() {
        return registerObject;
    }

    public void setRegisterObject(DeviceIdObject registerObject) {
        this.registerObject = registerObject;
    }
}
