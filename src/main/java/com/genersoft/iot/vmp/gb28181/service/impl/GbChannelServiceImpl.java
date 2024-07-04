package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GbChannelServiceImpl implements IGbChannelService {

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Override
    public CommonGBChannel queryByDeviceId(String gbDeviceId) {
        return commonGBChannelMapper.queryByDeviceId(gbDeviceId);
    }

    @Override
    public int add(CommonGBChannel commonGBChannel) {
        return 0;
    }

    @Override
    public int delete(int gbId) {
        return 0;
    }

    @Override
    public int update(CommonGBChannel commonGBChannel) {
        return 0;
    }

    @Override
    public int offline(CommonGBChannel commonGBChannel) {
        return 0;
    }

    @Override
    public int offline(List<CommonGBChannel> commonGBChannelList) {
        return 0;
    }

    @Override
    public int online(CommonGBChannel commonGBChannel) {
        return 0;
    }

    @Override
    public int online(List<CommonGBChannel> commonGBChannelList) {
        return 0;
    }

    @Override
    public void closeSend(CommonGBChannel commonGBChannel) {

    }

    @Override
    public void batchAdd(List<CommonGBChannel> commonGBChannels) {

    }

    @Override
    public void updateStatus(List<CommonGBChannel> channelList) {

    }

    @Override
    public List<CommonGBChannel> queryByPlatformId(Integer platformId) {
        return Collections.emptyList();
    }
}
