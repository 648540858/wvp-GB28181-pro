package com.genersoft.iot.vmp.gat1400.backend.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.backend.domain.container.ImageListObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageRequest {

    @JsonProperty("ImageListObject")
    private ImageListObject imageListObject;
}
