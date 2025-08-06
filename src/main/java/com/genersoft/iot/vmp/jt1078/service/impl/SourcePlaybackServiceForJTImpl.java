package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.CommonRecordInfo;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlaybackService;
import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.dao.JTChannelMapper;
import com.genersoft.iot.vmp.jt1078.dao.JTTerminalMapper;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service(ChannelDataType.PLAYBACK_SERVICE + ChannelDataType.JT_1078)
public class SourcePlaybackServiceForJTImpl implements ISourcePlaybackService {

    @Autowired
    private JTTerminalMapper jtDeviceMapper;

    @Autowired
    private JTChannelMapper jtChannelMapper;

    @Autowired
    private JT1078Template jt1078Template;

    @Autowired
    private Ijt1078PlayService playService;


    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(channel.getDataDeviceId());
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jtDeviceMapper.getDeviceById(jtChannel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());

        playService.playback(device.getPhoneNumber(), jtChannel.getChannelId(), DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime),
                DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime), 0, 0, 0, 0, result -> {
                    callback.run(result.getCode(), result.getMsg(), result.getData());
                });
    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(channel.getDataDeviceId());
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jtDeviceMapper.getDeviceById(jtChannel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());

        playService.stopPlayback(device.getPhoneNumber(), jtChannel.getChannelId());
    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(channel.getDataDeviceId());
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jtDeviceMapper.getDeviceById(jtChannel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());

        playService.playbackControl(device.getPhoneNumber(), jtChannel.getChannelId(), 1, 0, null);
    }

    @Override
    public void playbackResume(CommonGBChannel channel, String stream) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(channel.getDataDeviceId());
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jtDeviceMapper.getDeviceById(jtChannel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());

        playService.playbackControl(device.getPhoneNumber(), jtChannel.getChannelId(), 0, 0, null);
    }

    @Override
    public void playbackSeek(CommonGBChannel channel, String stream, long seekTime) {
        // 因为seek是增量，比如15s处， 1078是具体的时间点，比如2025-10-10 23:21:12 这个一个绝对时间点。无法转换，故此处不做支持
        log.warn("[JT-通用通道] 回放seek， 尚不支持");
    }

    @Override
    public void playbackSpeed(CommonGBChannel channel, String stream, Double speed) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(channel.getDataDeviceId());
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jtDeviceMapper.getDeviceById(jtChannel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());

        playService.playbackControl(device.getPhoneNumber(), jtChannel.getChannelId(), 0, (int)Math.floor(speed), null);
    }

    @Override
    public void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(channel.getDataDeviceId());
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jtDeviceMapper.getDeviceById(jtChannel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());
        List<J1205.JRecordItem> recordList = playService.getRecordList(device.getPhoneNumber(), jtChannel.getChannelId(), startTime, endTime);
        if (recordList.isEmpty()) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
            return;
        }

        List<CommonRecordInfo> recordInfoList = new ArrayList<>();
        for (J1205.JRecordItem jRecordItem : recordList) {
            CommonRecordInfo commonRecordInfo = new CommonRecordInfo();
            commonRecordInfo.setStartTime(jRecordItem.getStartTime());
            commonRecordInfo.setEndTime(jRecordItem.getEndTime());
            commonRecordInfo.setFileSize(jRecordItem.getSize() + "");
            recordInfoList.add(commonRecordInfo);
        }
        callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), recordInfoList);
    }
}
