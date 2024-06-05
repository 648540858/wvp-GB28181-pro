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
@Tag(name  = "部标设备管理")
@RequestMapping("/api/jt1078/device")
public class JT1078DeviceController {

    private final static Logger logger = LoggerFactory.getLogger(JT1078DeviceController.class);

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
    public void addDevice(Integer phoneNumber){
        assert phoneNumber != null;
        service.deleteDeviceByPhoneNumber(phoneNumber);
    }


    @Operation(summary = "1078-查询部标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "query", description = "查询内容")
    @GetMapping("/channel/list")
    public List<JTChannel> getChannels(@RequestParam(required = true) Integer phoneNumber,
                                       @RequestParam(required = false) String query) {
        assert phoneNumber != null;
        return service.getChannelList(phoneNumber, query);
    }
}

