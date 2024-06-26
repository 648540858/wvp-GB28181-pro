package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;

import java.util.List;

public interface IGbChannelService {

    CommonGBChannel queryByDeviceId(String gbDeviceId);

    int add(CommonGBChannel commonGBChannel);

    int delete(int gbId);

    int update(CommonGBChannel commonGBChannel);

    int offline(CommonGBChannel commonGBChannel);

    int online(CommonGBChannel commonGBChannel);

    void closeSend(CommonGBChannel commonGBChannel);

    void batchAdd(List<CommonGBChannel> commonGBChannels);

}
