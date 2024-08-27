package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 平台关联通道管理
 * @author lin
 */
public interface IPlatformChannelService {

    PageInfo<PlatformChannel> queryChannelList(int page, int count, String query, Boolean online, Integer platformId, Boolean hasShare);

    int addAllChannel(Integer platformId);

    int removeAllChannel(Integer platformId);

    int addChannels(Integer platformId, List<Integer> channelIds);

    int removeChannels(Integer platformId, List<Integer> channelIds);

    void removeChannels(List<CommonGBChannel> channelList);
}
