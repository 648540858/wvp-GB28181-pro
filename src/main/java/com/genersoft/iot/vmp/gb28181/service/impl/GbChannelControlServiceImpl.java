package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePTZService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GbChannelControlServiceImpl implements IGbChannelControlService {


    @Autowired
    private Map<String, ISourcePTZService> sourcePTZServiceMap;


    @Override
    public void ptz(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 云台控制， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持云台控制", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.ptz(channel, frontEndControlCode, callback);
    }

    @Override
    public void preset(CommonGBChannel channel, FrontEndControlCodeForPreset frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 预置位控制， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持预置位控制", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.preset(channel, frontEndControlCode, callback);
    }

    @Override
    public void fi(CommonGBChannel channel, FrontEndControlCodeForFI frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] FI指令， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持FI指令", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.fi(channel, frontEndControlCode, callback);
    }

    @Override
    public void tour(CommonGBChannel channel, FrontEndControlCodeForTour frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 巡航指令， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持巡航指令", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.tour(channel, frontEndControlCode, callback);
    }

    @Override
    public void scan(CommonGBChannel channel, FrontEndControlCodeForScan frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 扫描指令， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持扫描指令", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.scan(channel, frontEndControlCode, callback);
    }

    @Override
    public void auxiliary(CommonGBChannel channel, FrontEndControlCodeForAuxiliary frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 辅助开关控制指令， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持辅助开关控制指令", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.auxiliary(channel, frontEndControlCode, callback);
    }

    @Override
    public void wiper(CommonGBChannel channel, FrontEndControlCodeForWiper frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 雨刷控制， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持雨刷控制", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.wiper(channel, frontEndControlCode, callback);
    }

    @Override
    public void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {
        log.info("[通用通道] 预置位查询， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型： {} 不支持预置位查询", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.queryPreset(channel, callback);
    }
}
