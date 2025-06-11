package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;

@Data
public class SubscribeNotifications {

    @JsonProperty("SubscribeNotificationObject")
    private List<SubscribeNotificationObject> subscribeNotificationObject;
}
