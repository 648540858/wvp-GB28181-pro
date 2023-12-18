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
     * @param param
     */
    void save(StreamProxy param, GeneralCallback<StreamInfo> callback);

    /**
     * 添加视频代理到zlm
     * @param param
     * @return
     */
    JSONObject addStreamProxyToZlm(StreamProxy param);

    /**
     * 从zlm移除视频代理
     * @param param
     * @return
     */
    JSONObject removeStreamProxyFromZlm(StreamProxy param);

    /**
     * 分页查询
     * @param page
     * @param count
     * @return
     */
    PageInfo<StreamProxy> getAll(Integer page, Integer count);

    /**
     * 删除视频代理
     * @param app
     * @param stream
     */
    void del(String app, String stream);

    /**
     * 启用视频代理
     * @param app
     * @param stream
     * @return
     */
    boolean start(String app, String stream);

    /**
     * 更新状态
     * @param status 状态
     * @param app
     * @param stream
     */
    int updateStatus(boolean status, String app, String stream);



    /**
     * 停用用视频代理
     * @param app
     * @param stream
     * @return
     */
    boolean stop(String app, String stream);

    /**
     * 获取ffmpeg.cmd模板
     * @return
     */
    JSONObject getFFmpegCMDs(MediaServerItem mediaServerItem);

    /**
     * 根据app与stream获取streamProxy
     * @return
     */
    StreamProxy getStreamProxyByAppAndStream(String app, String streamId);


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

    /**
     * 更新代理流
     */
    boolean updateStreamProxy(StreamProxy streamProxyItem);

    /**
     * 获取统计信息
     * @return
     */
    ResourceBaseInfo getOverview();

    /**
     * 更新redis发来的gps更新消息
     */
    void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList);
}
