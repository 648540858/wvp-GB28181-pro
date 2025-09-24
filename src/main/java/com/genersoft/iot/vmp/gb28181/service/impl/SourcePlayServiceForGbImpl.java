package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;

@Slf4j
@Service(ChannelDataType.PLAY_SERVICE + ChannelDataType.GB28181)
public class SourcePlayServiceForGbImpl implements ISourcePlayService {

    @Autowired
    private IPlayService deviceChannelPlayService;

    @Override
    public void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback) {
        // 国标通道
        try {
            deviceChannelPlayService.play(channel, record, callback);
        } catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        } catch (ControllerException e) {
            log.error("[点播失败] {}({}), {}", channel.getGbName(), channel.getGbDeviceId(), e.getMsg());
            callback.run(Response.BUSY_HERE, "busy here", null);
        } catch (Exception e) {
            log.error("[点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlay(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            deviceChannelPlayService.stop(InviteSessionType.PLAY, channel, stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }
}
