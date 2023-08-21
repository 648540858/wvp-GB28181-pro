package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import org.springframework.stereotype.Service;

@Service
public class CommonGbChannelServiceImpl implements ICommonGbChannelService {
    @Override
    public CommonGbChannel getChannel(String channelId) {
        return null;
    }

    @Override
    public int add(CommonGbChannel channel) {
        return 0;
    }

    @Override
    public int delete(String channelId) {
        return 0;
    }

    @Override
    public int update(CommonGbChannel channel) {
        return 0;
    }

    @Override
    public boolean checkChannelInPlatform(String channelId, String platformServerId) {
        return false;
    }
}
