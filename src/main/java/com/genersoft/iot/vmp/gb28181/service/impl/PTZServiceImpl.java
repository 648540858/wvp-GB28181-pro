package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Preset;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PTZServiceImpl implements IPTZService {


    @Autowired
    private SIPCommander cmder;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IDeviceService deviceService;


    @Override
    public void ptz(Device device, String channelId, int cmdCode, int horizonSpeed, int verticalSpeed, int zoomSpeed) {
        try {
            cmder.frontEndCmd(device, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 云台控制: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public void frontEndCommand(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combindCode2) {
        // 判断设备是否属于当前平台, 如果不属于则发起自动调用
        if (!userSetting.getServerId().equals(device.getServerId())) {
            // 通道ID
            DeviceChannel deviceChannel = deviceChannelService.getOneForSource(device.getDeviceId(), channelId);
            Assert.notNull(deviceChannel, "通道不存在");
            String msg = redisRpcPlayService.frontEndCommand(device.getServerId(), deviceChannel.getId(), cmdCode, parameter1, parameter2, combindCode2);
            if (msg != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), msg);
            }
            return;
        }
        try {
            cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 前端控制: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public void frontEndCommand(CommonGBChannel channel, Integer cmdCode, Integer parameter1, Integer parameter2, Integer combindCode2) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只有国标通道的支持云台控制
            log.warn("[INFO 消息] 只有国标通道的支持云台控制， 通道ID： {}", channel.getGbId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持");
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备ID");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneById(channel.getGbId());
        frontEndCommand(device, deviceChannel.getDeviceId(), cmdCode, parameter1, parameter2, combindCode2);
    }

    @Override
    public List<Preset> queryPresetList(String deviceId, String channelDeviceId) {
        return Collections.emptyList();
    }

    @Override
    public void addPreset(Preset preset) {

    }

    @Override
    public void deletePreset(Integer qq) {

    }
}
