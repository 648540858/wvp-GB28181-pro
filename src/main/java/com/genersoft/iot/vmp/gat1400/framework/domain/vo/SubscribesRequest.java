package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeListObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeObject;

import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
public class SubscribesRequest {

    @JsonProperty("SubscribeListObject")
    private SubscribeListObject subscribeListObject;

    public static SubscribesRequest create(List<SubscribeObject> subscribes) {
        SubscribesRequest request = new SubscribesRequest();
        SubscribeListObject subscribeListObject = new SubscribeListObject();
        subscribeListObject.setSubscribeObject(subscribes);
        request.setSubscribeListObject(subscribeListObject);
        return request;
    }

    public static SubscribesRequest create(SubscribeObject subscribes) {
        SubscribesRequest request = new SubscribesRequest();
        SubscribeListObject subscribeListObject = new SubscribeListObject();
        subscribeListObject.setSubscribeObject(Collections.singletonList(subscribes));
        request.setSubscribeListObject(subscribeListObject);
        return request;
    }
}
