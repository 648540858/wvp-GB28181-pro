package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourceBroadcastService;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.AudioTalkResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ChannelDataType.BROADCAST_SERVICE + ChannelDataType.GB28181)
public class SourceBroadcastServiceForGbImpl implements ISourceBroadcastService {

    @Autowired
    private IPlayService playService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Override
    public AudioTalkResult startBroadcast(CommonGBChannel channel) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到设备");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到通道");
        }
        AudioBroadcastResult abResult = playService.audioBroadcast(
                device.getDeviceId(), deviceChannel.getDeviceId(), true);
        AudioTalkResult result = new AudioTalkResult();
        result.setPushStream(abResult.getStreamInfo());
        result.setPlayStream(null);
        return result;
    }

    @Override
    public void stopBroadcast(CommonGBChannel channel) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) return;
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) return;
        playService.stopAudioBroadcast(device, deviceChannel);
    }

    @Override
    public AudioTalkResult startTalk(CommonGBChannel channel) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到设备");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到通道");
        }
        AudioBroadcastResult abResult = playService.audioBroadcast(
                device.getDeviceId(), deviceChannel.getDeviceId(), false);
        AudioTalkResult result = new AudioTalkResult();
        result.setPushStream(abResult.getStreamInfo());
        result.setPlayStream(abResult.getPlayStreamInfo());
        return result;
    }

    @Override
    public void stopTalk(CommonGBChannel channel) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) return;
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) return;
        playService.stopTalk(device, deviceChannel, null);
    }
}
