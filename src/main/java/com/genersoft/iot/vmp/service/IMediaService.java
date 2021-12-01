package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

/**
 * 媒体信息业务
 */
public interface IMediaService {

    /**
     * 根据应用名和流ID获取播放地址, 通过zlm接口检查是否存在
     * @param app
     * @param stream
     * @return
     */
    StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId,String addr);


    /**
     * 根据应用名和流ID获取播放地址, 通过zlm接口检查是否存在, 返回的ip使用远程访问ip，适用与zlm与wvp在一台主机的情况
     * @param app
     * @param stream
     * @return
     */
    StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId);

    /**
     * 根据应用名和流ID获取播放地址, 只是地址拼接
     * @param app
     * @param stream
     * @return
     */
    StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaServerItem, String app, String stream, Object tracks);

    /**
     * 根据应用名和流ID获取播放地址, 只是地址拼接，返回的ip使用远程访问ip，适用与zlm与wvp在一台主机的情况
     * @param app
     * @param stream
     * @return
     */
    StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaInfo, String app, String stream, Object tracks, String addr);
}
