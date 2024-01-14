package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface IStreamProxyService {

    /**
     * 保存视频代理
     */
    void save(StreamProxy param, GeneralCallback<StreamInfo> callback);


    /**
     * 分页查询
     */
    PageInfo<StreamProxy> getAll(String query, Boolean online, String mediaServerId, Integer page, Integer count);


    /**
     * 启用视频代理
     */
    void start(String app, String stream, GeneralCallback<StreamInfo> callback);

    /**
     * 更新状态
     */
    void updateStatus(boolean status, String app, String stream);



    /**
     * 停用用视频代理
     */
    void stop(String app, String stream, GeneralCallback<StreamInfo> callback);

    /**
     * 获取ffmpeg.cmd模板
     */
    JSONObject getFFmpegCMDs(MediaServerItem mediaServerItem);

    /**
     * 根据app与stream获取streamProxy
     */
    StreamProxy getStreamProxyByAppAndStream(String app, String streamId);


    /**
     * 新的节点加入
     */
    void zlmServerOnline(String mediaServerId);

    /**
     * 节点离线
     */
    void zlmServerOffline(String mediaServerId);

    /**
     * 获取统计信息
     */
    ResourceBaseInfo getOverview();

    /**
     * 更新redis发来的gps更新消息
     */
    void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList);

    /**
     * 获取所有启用的拉流代理
     */
    List<StreamProxy> getAllForEnable();

    /**
     * 添加拉流代理
     */
    void add(StreamProxy param, GeneralCallback<StreamInfo> callback);

    /**
     *
     */
    void removeProxy(int id);

    /**
     * 编辑拉流代理
     */
    void edit(StreamProxy param, GeneralCallback<StreamInfo> callback);

    /**
     * 获取播放地址
     */
    void getStreamProxyById(Integer id, GeneralCallback<StreamInfo> callback);

}
