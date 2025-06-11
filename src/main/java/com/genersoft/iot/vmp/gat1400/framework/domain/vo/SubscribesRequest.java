package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

import cz.data.viid.framework.domain.dto.SubscribeListObject;
import cz.data.viid.framework.domain.dto.SubscribeObject;
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
