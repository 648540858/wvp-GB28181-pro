package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.CommonRecordInfo;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlaybackService;
import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Service(ChannelDataType.PLAYBACK_SERVICE + ChannelDataType.JT_1078)
public class SourcePlaybackServiceForJTImpl implements ISourcePlaybackService {

    @Autowired
    private Ijt1078PlayService playService;


    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {

    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {

    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {

    }

    @Override
    public void playbackResume(CommonGBChannel channel, String stream) {

    }

    @Override
    public void playbackSeek(CommonGBChannel channel, String stream, long seekTime) {

    }

    @Override
    public void playbackSpeed(CommonGBChannel channel, String stream, Double speed) {

    }

    @Override
    public void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback) {
        JTChannel jtChannel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jt1078Service.getDeviceById(channel.getTerminalDbId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());
        playService.getRecordList()
    }
}
