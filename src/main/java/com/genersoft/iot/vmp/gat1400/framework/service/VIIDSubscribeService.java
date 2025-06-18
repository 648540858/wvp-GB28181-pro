package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.fontend.domain.SubscribeQuery;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDSubscribeRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDSubscribe;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;


public interface VIIDSubscribeService extends IService<VIIDSubscribe> {

    VIIDSubscribe getCacheById(String subscribeId);

    VIIDResponseStatusObject subscribes(VIIDSubscribeRequest request);

    VIIDResponseStatusObject unSubscribes(String subscribeId);

    Page<VIIDSubscribe> list(SubscribeQuery request);
}
