package com.genersoft.iot.vmp.gat1400.backend.service;

import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeObject;

import java.util.List;


public interface ISubscribeService {

    ResponseStatusObject upsertSubscribes(SubscribeObject subscribeObject);

    ResponseStatusObject cancelSubscribes(String subscribeId);

    List<SubscribeObject> getSubscribes(SubscribeObject request);
}
