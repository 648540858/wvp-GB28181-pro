package com.genersoft.iot.vmp.jt1078.service.impl;

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
@Service(ChannelDataType.PLAYBACK_SERVICE + ChannelDataType.JT_1078)
public class SourcePlaybackServiceForJTImpl implements ISourcePlaybackService {


    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {

    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {

    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {

    }
}
