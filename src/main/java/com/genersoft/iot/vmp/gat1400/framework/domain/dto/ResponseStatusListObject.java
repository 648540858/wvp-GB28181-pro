package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
public class ResponseStatusListObject {

    @JsonProperty("ResponseStatusObject")
    private List<ResponseStatusObject> responseStatusObject;

    public ResponseStatusListObject() {}

    public ResponseStatusListObject(List<ResponseStatusObject> responseStatusObject) {
        this.responseStatusObject = responseStatusObject;
    }

    public ResponseStatusListObject(ResponseStatusObject responseStatusObject) {
        this.responseStatusObject = Collections.singletonList(responseStatusObject);
    }

}
