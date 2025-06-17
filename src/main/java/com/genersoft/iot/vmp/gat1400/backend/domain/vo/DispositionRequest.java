package com.genersoft.iot.vmp.gat1400.backend.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DispositionRequest {

    @JsonProperty("DispositionListObject")
    private com.genersoft.iot.vmp.gat1400.backend.domain.container.DispositionListObject DispositionListObject;
}
