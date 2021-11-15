package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.Page;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.github.pagehelper.PageInfo;

import java.util.List;

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
     * @param page
     * @param count
     * @return
     */
    Page<StreamPushItem> getPushList(Integer page, Integer count);
}
