package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;

import java.util.List;

/**
 * 平台关联通道管理
 * @author lin
 */
public interface IPlatformChannelService {

    /**
     * 更新目录下的通道
     * @param platformId 平台编号
     * @param channelReduces 通道信息
     * @param catalogId 目录编号
     * @return
     */
    int updateChannelForGB(String platformId, List<ChannelReduce> channelReduces, String catalogId);

    /**
     * 移除目录下的所有通道
     * @param platformId
     * @param catalogId
     * @return
     */
    int delAllChannelForGB(String platformId, String catalogId);
}
