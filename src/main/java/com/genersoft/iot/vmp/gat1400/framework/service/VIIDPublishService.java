package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.fontend.domain.PublishQuery;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDPublishRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;


public interface VIIDPublishService extends IService<VIIDPublish> {

    Page<VIIDPublish> page(PublishQuery request);

    boolean addPublish(VIIDPublishRequest request);

    boolean refreshPublish(VIIDPublishRequest request);
}
