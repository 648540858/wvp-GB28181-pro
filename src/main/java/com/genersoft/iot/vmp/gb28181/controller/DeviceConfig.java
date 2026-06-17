package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Tag(name = "国标设备配置")
@RestController
@RequestMapping("/api/device/config")
public class DeviceConfig {

    @Autowired
    private IDeviceService deviceService;

    @GetMapping("/set/basicParam")
    @Operation(summary = "设置基本配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "basicParam", description = "基础配置参数", required = true)
    public DeferredResult<WVPResult<String>> homePositionApi(BasicParam basicParam) {
        if (log.isDebugEnabled()) {
            log.debug("基本配置设置命令API调用");
        }
        Assert.notNull(basicParam.getDeviceId(), "设备ID必须存在");

        Device device = deviceService.getDeviceByDeviceId(basicParam.getDeviceId());
        Assert.notNull(device, "设备不存在");

        DeferredResult<WVPResult<String>> deferredResult = new DeferredResult<>();
        deviceService.deviceBasicConfig(device, basicParam, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });

        deferredResult.onTimeout(() -> {
            log.warn("[设备配置] 超时, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
        });
        return deferredResult;
    }

    @Operation(summary = "查询基本参数配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号")
    @GetMapping("/query/basicParam")
    public DeferredResult<WVPResult<BasicParam>> queryBasicParam(String deviceId,
                                                                  @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "设备不存在");
        DeferredResult<WVPResult<BasicParam>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, BasicParam.class, (code, msg, data) -> {
            data.setDeviceId(deviceId);
            data.setChannelId(channelId);
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[获取设备配置] 超时, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
        });
        return deferredResult;
    }

    @Operation(summary = "查询视频参数范围", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号")
    @GetMapping("/query/videoParamOpt")
    public DeferredResult<WVPResult<VideoParamOpt>> queryVideoParamOpt(String deviceId,
                                                                        @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "设备不存在");
        DeferredResult<WVPResult<VideoParamOpt>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, VideoParamOpt.class, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[获取设备配置] 超时, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
        });
        return deferredResult;
    }

    @Operation(summary = "查询SVAC编码配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号")
    @GetMapping("/query/svacEncodeConfig")
    public DeferredResult<WVPResult<SVACEncodeConfig>> querySVACEncodeConfig(String deviceId,
                                                                              @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "设备不存在");
        DeferredResult<WVPResult<SVACEncodeConfig>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, SVACEncodeConfig.class, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[获取设备配置] 超时, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
        });
        return deferredResult;
    }

    @Operation(summary = "查询SVAC解码配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号")
    @GetMapping("/query/svacDecodeConfig")
    public DeferredResult<WVPResult<SVACDecodeConfig>> querySVACDecodeConfig(String deviceId,
                                                                              @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "设备不存在");
        DeferredResult<WVPResult<SVACDecodeConfig>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, SVACDecodeConfig.class, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[获取设备配置] 超时, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
        });
        return deferredResult;
    }

}
