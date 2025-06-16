package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotifications;

import lombok.Data;

@Data
public class SubscribeNotificationRequest {

    @JsonProperty("SubscribeNotificationListObject")
    private SubscribeNotifications subscribeNotificationListObject;
}
