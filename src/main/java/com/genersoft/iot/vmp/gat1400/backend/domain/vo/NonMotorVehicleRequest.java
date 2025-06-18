package com.genersoft.iot.vmp.gat1400.backend.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.NonMotorVehicleObjectList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NonMotorVehicleRequest {

    @JsonProperty("NonMotorVehicleListObject")
    private NonMotorVehicleObjectList NonMotorVehicleListObject;
}
