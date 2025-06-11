package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cz.data.viid.fe.domain.SubscribeQuery;
import cz.data.viid.fe.domain.VIIDSubscribeRequest;
import cz.data.viid.framework.domain.entity.VIIDSubscribe;
import cz.data.viid.framework.domain.vo.VIIDResponseStatusObject;

public interface VIIDSubscribeService extends IService<VIIDSubscribe> {

    VIIDSubscribe getCacheById(String subscribeId);

    VIIDResponseStatusObject subscribes(VIIDSubscribeRequest request);

    VIIDResponseStatusObject unSubscribes(String subscribeId);

    Page<VIIDSubscribe> list(SubscribeQuery request);
}
