package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlaybackService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;

@Slf4j
@Service(ChannelDataType.PLAYBACK_SERVICE + ChannelDataType.GB28181)
public class SourcePlaybackServiceForGbImpl implements ISourcePlaybackService {

    @Autowired
    private IPlayService deviceChannelPlayService;

    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        try {
            deviceChannelPlayService.playBack(channel, startTime, stopTime, callback);
        } catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        } catch (Exception e) {
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            deviceChannelPlayService.stop(InviteSessionType.PLAYBACK, channel, stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {

    }
}
