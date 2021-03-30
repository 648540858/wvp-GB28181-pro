package com.genersoft.iot.vmp.vmanager.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyDto;
import com.genersoft.iot.vmp.vmanager.streamProxy.StreamProxyController;
import com.github.pagehelper.PageInfo;

public interface IStreamProxyService {

    /**
     * 保存视频代理
     * @param param
     */
    void save(StreamProxyDto param);

    /**
     * 添加视频代理到zlm
     * @param param
     * @return
     */
    JSONObject addStreamProxyToZlm(StreamProxyDto param);

    /**
     * 从zlm移除视频代理
     * @param param
     * @return
     */
    JSONObject removeStreamProxyFromZlm(StreamProxyDto param);

    /**
     * 分页查询
     * @param page
     * @param count
     * @return
     */
    PageInfo<StreamProxyDto> getAll(Integer page, Integer count);

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
     * 停用用视频代理
     * @param app
     * @param stream
     * @return
     */
    boolean stop(String app, String stream);
}
