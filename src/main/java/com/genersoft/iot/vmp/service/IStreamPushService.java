package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface IStreamPushService {

    List<StreamPushItem> handleJSON(String json, MediaServerItem mediaServerItem);

    /**
     * 将应用名和流ID加入国标关联
     * @param stream
     * @return
     */
    boolean saveToGB(GbStream stream);

    /**
     * 将应用名和流ID移出国标关联
     * @param stream
     * @return
     */
    boolean removeFromGB(GbStream stream);

    /**
     * 获取
     */
    PageInfo<StreamPushItem> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId);

    List<StreamPushItem> getPushList(String mediaSererId);

    StreamPushItem transform(MediaItem item);

    StreamPushItem getPush(String app, String streamId);

    /**
     * 停止一路推流
     * @param app 应用名
     * @param streamId 流ID
     * @return
     */
    boolean stop(String app, String streamId);

    /**
     * 新的节点加入
     * @param mediaServerId
     * @return
     */
    void zlmServerOnline(String mediaServerId);

    /**
     * 节点离线
     * @param mediaServerId
     * @return
     */
    void zlmServerOffline(String mediaServerId);

    void clean();

    boolean saveToRandomGB();

    void batchAdd(List<StreamPushItem> streamPushExcelDtoList);

    boolean batchStop(List<GbStream> streamPushItems);

    void batchAddForUpload(List<StreamPushItem> streamPushItems, Map<String, List<String[]>> streamPushItemsForAll);
}
