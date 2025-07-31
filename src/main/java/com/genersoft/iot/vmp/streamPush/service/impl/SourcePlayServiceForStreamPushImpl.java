package com.genersoft.iot.vmp.streamPush.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;

@Slf4j
@Service(ChannelDataType.PLAY_SERVICE + ChannelDataType.STREAM_PUSH)
public class SourcePlayServiceForStreamPushImpl implements ISourcePlayService {

    @Autowired
    private IStreamPushPlayService playService;

    @Override
    public void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback) {
        String serverGBId = null;
        String platformName = null;
        if (platform != null) {
            // 推流
            serverGBId = platform.getServerGBId();
            platformName = platform.getName();
        }
        // 推流
        try {
            playService.start(channel.getDataDeviceId(), callback, serverGBId, platformName);
        }catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        }catch (Exception e) {
            log.error("[点播推流通道失败] 通道： {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlay(CommonGBChannel channel, String stream) {
        // 推流
        try {
            playService.stop(channel.getDataDeviceId());
        }catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }
}
