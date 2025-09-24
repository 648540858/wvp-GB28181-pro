package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePTZService;
import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Service(ChannelDataType.PTZ_SERVICE + ChannelDataType.JT_1078)
public class SourcePTZServiceForJTImpl implements ISourcePTZService {

    @Autowired
    private Ijt1078Service service;

    @Autowired
    private JT1078Template jt1078Template;

    @Override
    public void ptz(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        JTChannel jtChannel = service.getChannelByDbId(channel.getDataDeviceId());
        if (jtChannel == null) {
            callback.run(ErrorCode.ERROR404.getCode(), "通道不存在", null);
            return;
        }
        JTDevice jtDevice = service.getDeviceById(jtChannel.getTerminalDbId());
        if (jtDevice == null) {
            callback.run(ErrorCode.ERROR404.getCode(), "设备不存在", null);
            return;
        }

        if (frontEndControlCode.getPan() == null && frontEndControlCode.getTilt() == null && frontEndControlCode.getZoom() == null) {
            J9301 j9301 = new J9301();
            j9301.setChannel(jtChannel.getChannelId());
            j9301.setDirection(0);
            j9301.setSpeed(0);
            jt1078Template.ptzRotate(jtDevice.getPhoneNumber(), j9301, 6);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
            return;
        }

        if (frontEndControlCode.getPan() != null || frontEndControlCode.getTilt() != null) {
            J9301 j9301 = new J9301();
            j9301.setChannel(jtChannel.getChannelId());
            if (frontEndControlCode.getTilt() != null) {
                if (frontEndControlCode.getTilt() == 0) {
                    j9301.setDirection(1);
                }else if (frontEndControlCode.getTilt() == 1) {
                    j9301.setDirection(2);
                }
                j9301.setSpeed((int)(frontEndControlCode.getTilt()/100D * 255));
            }

            if (frontEndControlCode.getPan() != null) {
                if (frontEndControlCode.getPan() == 0) {
                    j9301.setDirection(3);
                }else if (frontEndControlCode.getPan() == 1) {
                    j9301.setDirection(4);
                }
                j9301.setSpeed((int)(frontEndControlCode.getPanSpeed()/100D * 255));
            }
            jt1078Template.ptzRotate(jtDevice.getPhoneNumber(), j9301, 6);
        }
        if (frontEndControlCode.getZoom() != null) {
            J9306 j9306 = new J9306();
            j9306.setChannel(jtChannel.getChannelId());
            j9306.setZoom(1 - frontEndControlCode.getZoom());
            jt1078Template.ptzZoom(jtDevice.getPhoneNumber(), j9306, 6);
        }
        callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    @Override
    public void preset(CommonGBChannel channel, FrontEndControlCodeForPreset frontEndControlCode, ErrorCallback<String> callback) {
        callback.run(ErrorCode.ERROR486.getCode(), ErrorCode.ERROR486.getMsg(), null);
    }

    @Override
    public void fi(CommonGBChannel channel, FrontEndControlCodeForFI frontEndControlCode, ErrorCallback<String> callback) {
        JTChannel jtChannel = service.getChannelByDbId(channel.getDataDeviceId());
        if (jtChannel == null) {
            callback.run(ErrorCode.ERROR404.getCode(), "通道不存在", null);
            return;
        }
        JTDevice jtDevice = service.getDeviceById(jtChannel.getTerminalDbId());
        if (jtDevice == null) {
            callback.run(ErrorCode.ERROR404.getCode(), "设备不存在", null);
            return;
        }
        if (frontEndControlCode.getIris() != null) {
            J9303 j9303 = new J9303();
            j9303.setChannel(jtChannel.getChannelId());
            j9303.setIris(1 - frontEndControlCode.getIris());
            jt1078Template.ptzIris(jtDevice.getPhoneNumber(), j9303, 6);
        }
        if (frontEndControlCode.getFocus() != null) {
            J9302 j9302 = new J9302();
            j9302.setChannel(jtChannel.getChannelId());
            j9302.setFocalDirection(1 - frontEndControlCode.getFocus());
            jt1078Template.ptzFocal(jtDevice.getPhoneNumber(), j9302, 6);
        }
    }

    @Override
    public void tour(CommonGBChannel channel, FrontEndControlCodeForTour frontEndControlCode, ErrorCallback<String> callback) {
        callback.run(ErrorCode.ERROR486.getCode(), ErrorCode.ERROR486.getMsg(), null);
    }

    @Override
    public void scan(CommonGBChannel channel, FrontEndControlCodeForScan frontEndControlCode, ErrorCallback<String> callback) {
        callback.run(ErrorCode.ERROR486.getCode(), ErrorCode.ERROR486.getMsg(), null);
    }

    @Override
    public void auxiliary(CommonGBChannel channel, FrontEndControlCodeForAuxiliary frontEndControlCode, ErrorCallback<String> callback) {
        callback.run(ErrorCode.ERROR486.getCode(), ErrorCode.ERROR486.getMsg(), null);
    }

    @Override
    public void wiper(CommonGBChannel channel, FrontEndControlCodeForWiper frontEndControlCode, ErrorCallback<String> callback) {
        JTChannel jtChannel = service.getChannelByDbId(channel.getDataDeviceId());
        if (jtChannel == null) {
            callback.run(ErrorCode.ERROR404.getCode(), "通道不存在", null);
            return;
        }
        JTDevice jtDevice = service.getDeviceById(jtChannel.getTerminalDbId());
        if (jtDevice == null) {
            callback.run(ErrorCode.ERROR404.getCode(), "设备不存在", null);
            return;
        }
        J9304 j9304 = new J9304();
        j9304.setChannel(jtChannel.getChannelId());
        if (frontEndControlCode.getCode() == 1) {
            j9304.setOn(1);
        }else if (frontEndControlCode.getCode() == 0){
            j9304.setOn(0);
        }
        jt1078Template.ptzWiper(jtDevice.getPhoneNumber(), j9304, 6);
    }

    @Override
    public void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {
        callback.run(ErrorCode.ERROR486.getCode(), ErrorCode.ERROR486.getMsg(), null);
    }
}
