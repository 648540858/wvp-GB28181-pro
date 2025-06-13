package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.fontend.domain.ServerQuery;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;

import java.util.List;


public interface VIIDServerService extends IService<VIIDServer> {

    void afterPropertiesSet();

    VIIDServer getByIdAndEnabled(String id);

    VIIDServer getCurrentServer(String useCache);

    VIIDServer getCurrentServer();

    Page<VIIDServer> page(ServerQuery request);

    Page<VIIDServer> list(ServerQuery request);

    boolean upsert(VIIDServer request);

    void changeDomain(String source);

    boolean maintenance(VIIDServer viidServer);

    List<VIIDServer> activeKeepaliveServer();

    void instanceStatus(String serverId, Constants.DeviceStatus status);
}
