package com.genersoft.iot.vmp.gat1400.backend.service;

import java.util.List;

import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.dto.SubscribeObject;

public interface ISubscribeService {

    ResponseStatusObject upsertSubscribes(SubscribeObject subscribeObject);

    ResponseStatusObject cancelSubscribes(String subscribeId);

    List<SubscribeObject> getSubscribes(SubscribeObject request);
}
