package com.genersoft.iot.vmp.gat1400.backend.domain.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.backend.domain.dto.ImageObject;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageListObject {

    @JsonProperty("Image")
    private List<ImageObject> image;
}
