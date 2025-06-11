package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

import cz.data.viid.fe.domain.ServerQuery;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.VIIDServer;

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
