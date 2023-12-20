package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.IResourcePlayCallback;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import org.springframework.stereotype.Service;

@Service(CommonGbChannelType.PROXY)
public class StreamProxyResourceServiceImpl implements IResourceService {
    @Override
    public boolean deleteChannel(CommonGbChannel commonGbChannel) {
        return false;
    }

    @Override
    public void startPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {

    }

    @Override
    public void stopPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {

    }

    @Override
    public boolean ptzControl(CommonGbChannel commonGbChannel, String command, Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed) {
        return false;
    }

    @Override
    public void streamOffline(String app, String streamId) {

    }

    @Override
    public void startPlayback(CommonGbChannel channel, Long startTime, Long stopTime, IResourcePlayCallback callback) {

    }

    @Override
    public void startDownload(CommonGbChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback) {

    }
}
