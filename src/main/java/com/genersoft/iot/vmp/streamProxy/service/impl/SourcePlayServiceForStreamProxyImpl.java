package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;

@Slf4j
@Service(ChannelDataType.PLAY_SERVICE + ChannelDataType.STREAM_PROXY)
public class SourcePlayServiceForStreamProxyImpl implements ISourcePlayService {

    @Autowired
    private IStreamProxyPlayService playService;

    @Override
    public void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback) {
        // 拉流代理通道
        try {
            playService.start(channel.getDataDeviceId(), record, callback);
        }catch (Exception e) {
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlay(CommonGBChannel channel, String stream) {
        // 拉流代理通道
        try {
            playService.stop(channel.getDataDeviceId());
        }catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }
}
