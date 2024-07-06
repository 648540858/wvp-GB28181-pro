package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;

import java.util.List;

public interface IGbChannelService {

    CommonGBChannel queryByDeviceId(String gbDeviceId);

    int add(CommonGBChannel commonGBChannel);

    int delete(int gbId);

    int update(CommonGBChannel commonGBChannel);

    int offline(CommonGBChannel commonGBChannel);

    int offline(List<CommonGBChannel> commonGBChannelList);

    int online(CommonGBChannel commonGBChannel);

    int online(List<CommonGBChannel> commonGBChannelList);

    void closeSend(CommonGBChannel commonGBChannel);

    void closeSend(List<CommonGBChannel> commonGBChannelList);

    void batchAdd(List<CommonGBChannel> commonGBChannels);

    void updateStatus(List<CommonGBChannel> channelList);

    List<CommonGBChannel> queryByPlatformId(Integer platformId);
}
