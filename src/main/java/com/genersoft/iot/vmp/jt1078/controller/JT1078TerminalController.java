package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@Tag(name  = "部标终端以及通道管理")
@RequestMapping("/api/jt1078/terminal")
public class JT1078TerminalController {

    private final static Logger logger = LoggerFactory.getLogger(JT1078TerminalController.class);

    @Resource
    Ijt1078Service service;

    @Autowired
    UserSetting userSetting;

    @Operation(summary = "1078-分页查询部标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @GetMapping("/list")
    public PageInfo<JTDevice> getDevices(int page, int count,
                                         @RequestParam(required = false) String query,
                                         @RequestParam(required = false) Boolean online) {
        return service.getDeviceList(page, count, query, online);
    }

    @Operation(summary = "更新设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "device", description = "设备", required = true)
    @PostMapping("/update")
    public void updateDevice(JTDevice device){
        assert device.getId() > 0;
        assert device.getPhoneNumber() != null;
        service.updateDevice(device);
    }

    @Operation(summary = "1078-新增设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "device", description = "设备", required = true)
    @PostMapping("/add")
    public void addDevice(JTDevice device){
        assert device.getPhoneNumber() != null;
        service.addDevice(device);
    }
    @Operation(summary = "删除设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @DeleteMapping("/delete")
    public void addDevice(String phoneNumber){
        assert phoneNumber != null;
        service.deleteDeviceByPhoneNumber(phoneNumber);
    }
    @Operation(summary = "查询设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @GetMapping("/query")
    public JTDevice getDevice(Integer deviceId){
        return service.getDeviceById(deviceId);
    }


    @Operation(summary = "1078-查询部标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "deviceId", description = "设备ID", required = true)
    @Parameter(name = "query", description = "查询内容")
    @GetMapping("/channel/list")
    public PageInfo<JTChannel> getChannels(int page, int count,
                                           @RequestParam(required = true) Integer deviceId,
                                           @RequestParam(required = false) String query) {
        assert deviceId != null;
        return service.getChannelList(page, count, deviceId, query);
    }

    @Operation(summary = "1078-更新通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channel", description = "通道", required = true)
    @PostMapping("/channel/update")
    public void updateChannel(JTChannel channel){
        assert channel.getId() > 0;
        assert channel.getChannelId() != null;
        service.updateChannel(channel);
    }

    @Operation(summary = "1078-新增通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channel", description = "通道", required = true)
    @PostMapping("/channel/add")
    public void addChannel(JTChannel channel){
        assert channel.getChannelId() != null;
        service.addChannel(channel);
    }
    @Operation(summary = "1078-删除通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "通道的数据库ID", required = true)
    @DeleteMapping("/channel/delete")
    public void deleteChannel(Integer id){
        service.deleteChannelById(id);
    }
}

