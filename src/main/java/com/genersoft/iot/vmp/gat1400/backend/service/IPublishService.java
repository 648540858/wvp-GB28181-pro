package com.genersoft.iot.vmp.gat1400.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;

import java.util.List;


public interface IPublishService extends IService<VIIDPublish> {

    List<VIIDPublish> findListByServerId(String serverId, Integer status);

    boolean updateSubscribe(String id, Constants.SubscribeStatus status);
}
