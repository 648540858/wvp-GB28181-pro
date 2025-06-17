package com.genersoft.iot.vmp.gat1400.backend.domain.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.genersoft.iot.vmp.gat1400.backend.domain.dto.DispositionObject;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DispositionListObject {

    @JsonProperty("DispositionObject")
    private List<DispositionObject> DispositionObject;
}
