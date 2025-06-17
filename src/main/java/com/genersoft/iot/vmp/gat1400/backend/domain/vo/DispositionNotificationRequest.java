package com.genersoft.iot.vmp.gat1400.backend.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.backend.domain.container.DispositionNotificationListObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositionNotificationRequest {

    @JsonProperty("DispositionNotificationListObject")
    private DispositionNotificationListObject DispositionNotificationListObject;
}
