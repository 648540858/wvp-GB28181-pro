package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;

@Data
public class LaneObjectList {

    @JsonProperty("LaneObject")
    private List<LaneObject> laneObject;
}
