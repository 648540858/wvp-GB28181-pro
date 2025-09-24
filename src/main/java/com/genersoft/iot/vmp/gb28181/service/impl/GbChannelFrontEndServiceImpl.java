package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelFrontEndService;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sip.message.Response;
import java.util.List;

@Slf4j
@Service
public class GbChannelFrontEndServiceImpl implements IGbChannelFrontEndService {

    @Autowired
    private IPTZService ptzService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    private void frontEndCommand(CommonGBChannel channel, Integer cmdCode, Integer parameter1, Integer parameter2, Integer combindCode2){

        Assert.isTrue(channel.getDataType() == ChannelDataType.GB28181, "类型获取错误");

        Device device = deviceService.getDevice(channel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        Assert.notNull(device, "原始通道不存在");

        ptzService.frontEndCommand(device, deviceChannel.getDeviceId(), cmdCode, parameter1, parameter2, combindCode2);
    }

    @Override
    public void ptz(CommonGBChannel channel, String command, Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int cmdCode = 0;
            switch (command){
                case "left":
                    cmdCode = 2;
                    break;
                case "right":
                    cmdCode = 1;
                    break;
                case "up":
                    cmdCode = 8;
                    break;
                case "down":
                    cmdCode = 4;
                    break;
                case "upleft":
                    cmdCode = 10;
                    break;
                case "upright":
                    cmdCode = 9;
                    break;
                case "downleft":
                    cmdCode = 6;
                    break;
                case "downright":
                    cmdCode = 5;
                    break;
                case "zoomin":
                    cmdCode = 16;
                    break;
                case "zoomout":
                    cmdCode = 32;
                    break;
                case "stop":
                    horizonSpeed = 0;
                    verticalSpeed = 0;
                    zoomSpeed = 0;
                    break;
                default:
                    break;
            }
            frontEndCommand(channel, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);

        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void iris(CommonGBChannel channel, String command, Integer speed) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int cmdCode = 0x40;
            switch (command){
                case "in":
                    cmdCode = 0x44;
                    break;
                case "out":
                    cmdCode = 0x48;
                    break;
                case "stop":
                    speed = 0;
                    break;
                default:
                    break;
            }
            frontEndCommand(channel, cmdCode, 0, speed, 0);

        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void focus(CommonGBChannel channel, String command, Integer speed) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int cmdCode = 0x40;
            switch (command){
                case "near":
                    cmdCode = 0x42;
                    break;
                case "far":
                    cmdCode = 0x41;
                    break;
                case "stop":
                    speed = 0;
                    break;
                default:
                    break;
            }
            frontEndCommand(channel, cmdCode, speed, 0, 0);

        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }

    }

    @Override
    public void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道

            Device device = deviceService.getDevice(channel.getDataDeviceId());
            Assert.notNull(device, "设备不存在");

            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
            Assert.notNull(device, "原始通道不存在");

            deviceService.queryPreset(device, deviceChannel.getDeviceId(), callback);

        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void addPreset(CommonGBChannel channel, Integer presetId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x81, 1, presetId, 0);

        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void callPreset(CommonGBChannel channel, Integer presetId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x82, 1, presetId, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void deletePreset(CommonGBChannel channel, Integer presetId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x83, 1, presetId, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void addCruisePoint(CommonGBChannel channel, Integer cruiseId, Integer presetId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x84, cruiseId, presetId, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void deleteCruisePoint(CommonGBChannel channel, Integer cruiseId, Integer presetId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x85, cruiseId, presetId, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void setCruiseSpeed(CommonGBChannel channel, Integer cruiseId, Integer speed) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int parameter2 = speed & 0xFF;
            int combindCode2 =  speed >> 8;
            frontEndCommand(channel, 0x86, cruiseId, parameter2, combindCode2);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void setCruiseTime(CommonGBChannel channel, Integer cruiseId, Integer time) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int parameter2 = time & 0xFF;
            int combindCode2 =  time >> 8;
            frontEndCommand(channel, 0x87, cruiseId, parameter2, combindCode2);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void startCruise(CommonGBChannel channel, Integer cruiseId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x88, cruiseId, 0, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void stopCruise(CommonGBChannel channel, Integer cruiseId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0, 0, 0, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void startScan(CommonGBChannel channel, Integer scanId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x89, scanId, 0, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void stopScan(CommonGBChannel channel, Integer scanId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0, 0, 0, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void setScanLeft(CommonGBChannel channel, Integer scanId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x89, scanId, 1, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void setScanRight(CommonGBChannel channel, Integer scanId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            frontEndCommand(channel, 0x89, scanId, 2, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void setScanSpeed(CommonGBChannel channel, Integer scanId, Integer speed) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int parameter2 = speed & 0xFF;
            int combindCode2 =  speed >> 8;
            frontEndCommand(channel, 0x8A, scanId, parameter2, combindCode2);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void wiper(CommonGBChannel channel, String command) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int cmdCode = 0;
            switch (command){
                case "on":
                    cmdCode = 0x8c;
                    break;
                case "off":
                    cmdCode = 0x8d;
                    break;
                default:
                    break;
            }
            frontEndCommand(channel, cmdCode, 1, 0, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void auxiliarySwitch(CommonGBChannel channel, String command, Integer switchId) {
        Assert.notNull(channel, "通道不存在");
        if (channel.getDataType() == ChannelDataType.GB28181) {
            // 国标通道
            int cmdCode = 0;
            switch (command){
                case "on":
                    cmdCode = 0x8c;
                    break;
                case "off":
                    cmdCode = 0x8d;
                    break;
                default:
                    break;
            }
            frontEndCommand(channel, cmdCode, switchId, 0, 0);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH) {
            // 拉流代理
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持此操作");
        } else {
            // 通道数据异常
            log.error("[通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }
}
