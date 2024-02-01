package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPush;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
public interface IStreamPushService {

    List<StreamPush> handleJSON(String json, MediaServerItem mediaServerItem);

    /**
     * 获取
     */
    PageInfo<StreamPush> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId);

    List<StreamPush> getPushList(String mediaSererId);

    StreamPush transform(OnStreamChangedHookParam item);

    StreamPush getPush(String app, String streamId);

    /**
     * 停止一路推流
     * @param app 应用名
     * @param streamId 流ID
     */
    boolean stop(String app, String streamId);

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

    /**
     * 批量添加
     */
    void batchAdd(List<StreamPush> streamPushExcelDtoList);

    /**
     * 中止多个推流
     */
    boolean batchStop(List<Integer> streamPushIds);

    /**
     * 导入时批量增加
     */
    void batchAddForUpload(List<StreamPushExcelDto> streamPushItems);

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
    boolean add(StreamPush stream);

    /**
     * 获取全部的app+Streanm 用于判断推流列表是新增还是修改
     * @return
     */
    Map<String, StreamPush> getAllAppAndStream();

    /**
     * 获取统计信息
     * @return
     */
    ResourceBaseInfo getOverview();

    void batchUpdate(List<StreamPush> streamPushItemForUpdate);

    boolean update(StreamPush transform);

    /**
     * 更新redis发来的gps更新消息
     */
    void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList);

    /**
     * 移除推流信息
     */
    boolean remove(Integer id);

    /**
     * 设置推流离线
     */
    void offline(String app, String stream);

    /**
     *
     */
    StreamPush getPushByCommonChannelId(int commonGbId);
}
