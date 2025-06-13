package com.genersoft.iot.vmp.gat1400.backend.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositionNotificationRequest {

    @JsonProperty("DispositionNotificationListObject")
    private cz.data.viid.be.domain.container.DispositionNotificationListObject DispositionNotificationListObject;
}
