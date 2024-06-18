package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
public interface IStreamPushService {

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

    StreamPushItem transform(OnStreamChangedHookParam item);

    StreamPushItem getPush(String app, String streamId);

    /**
     * 停止一路推流
     * @param app 应用名
     * @param stream 流ID
     */
    boolean stop(String app, String stream);

    /**
     * 新的节点加入
     */
    void zlmServerOnline(String mediaServerId);

    /**
     * 节点离线
     */
    void zlmServerOffline(String mediaServerId);

    /**
     * 清空
     */
    void clean();


    boolean saveToRandomGB();

    /**
     * 批量添加
     */
    void batchAdd(List<StreamPushItem> streamPushExcelDtoList);

    /**
     * 中止多个推流
     */
    boolean batchStop(List<GbStream> streamPushItems);

    /**
     * 导入时批量增加
     */
    void batchAddForUpload(List<StreamPushItem> streamPushItems, Map<String, List<String[]>> streamPushItemsForAll);

    /**
     * 全部离线
     */
    void allStreamOffline();

    /**
     * 推流离线
     */
    void offline(List<StreamPushItemFromRedis> offlineStreams);

    /**
     * 推流上线
     */
    void online(List<StreamPushItemFromRedis> onlineStreams);

    /**
     * 增加推流
     */
    boolean add(StreamPushItem stream);

    /**
     * 获取全部的app+Streanm 用于判断推流列表是新增还是修改
     * @return
     */
    List<String> getAllAppAndStream();

    /**
     * 获取统计信息
     * @return
     */
    ResourceBaseInfo getOverview();

    Map<String, StreamPushItem> getAllAppAndStreamMap();


    void updatePush(OnStreamChangedHookParam param);
}
