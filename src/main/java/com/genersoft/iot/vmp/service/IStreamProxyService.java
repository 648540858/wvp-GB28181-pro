package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.github.pagehelper.PageInfo;

public interface IStreamProxyService {

    /**
     * 保存视频代理
     * @param param
     */
    String save(StreamProxyItem param);

    /**
     * 添加视频代理到zlm
     * @param param
     * @return
     */
    JSONObject addStreamProxyToZlm(StreamProxyItem param);

    /**
     * 从zlm移除视频代理
     * @param param
     * @return
     */
    JSONObject removeStreamProxyFromZlm(StreamProxyItem param);

    /**
     * 分页查询
     * @param page
     * @param count
     * @return
     */
    PageInfo<StreamProxyItem> getAll(Integer page, Integer count);

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
