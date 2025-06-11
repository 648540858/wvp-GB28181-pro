package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cz.data.viid.fe.domain.PublishQuery;
import cz.data.viid.fe.domain.VIIDPublishRequest;
import cz.data.viid.framework.domain.entity.VIIDPublish;

public interface VIIDPublishService extends IService<VIIDPublish> {

    Page<VIIDPublish> page(PublishQuery request);

    boolean addPublish(VIIDPublishRequest request);

    boolean refreshPublish(VIIDPublishRequest request);
}
