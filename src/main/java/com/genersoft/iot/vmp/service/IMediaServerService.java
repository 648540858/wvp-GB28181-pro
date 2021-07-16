package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.IMediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

import java.util.List;

/**
 * 媒体服务节点
 */
public interface IMediaServerService {

    List<IMediaServerItem> getAll();

    IMediaServerItem getOne(String generalMediaServerId);

    IMediaServerItem getOneByHostAndPort(String host, int port);

    /**
     * 新的节点加入
     * @param zlmServerConfig
     * @return
     */
    void handLeZLMServerConfig(ZLMServerConfig zlmServerConfig);

    void updateServerCatch(IMediaServerItem mediaServerItem, Integer count, Boolean b);

    IMediaServerItem getMediaServerForMinimumLoad();

    void setZLMConfig(IMediaServerItem mediaServerItem);

    void init();

    void closeRTPServer(Device device, String channelId);

    void update(MediaConfig mediaConfig);

    void addCount(String mediaServerId);

    void removeCount(String mediaServerId);
}
