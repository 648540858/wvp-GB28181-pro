package com.genersoft.iot.vmp.vmanager.channel;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "通用国标通道")

@RestController
@RequestMapping("/api/channel")
public class CommonChannelController {

    private final static Logger logger = LoggerFactory.getLogger(CommonChannelController.class);

    @Autowired
    private ICommonGbChannelService commonGbChannelService;


    /**
     * 查询区域下的通道
     *
     * @param civilCode 区域编号
     */
    @DeleteMapping("/region/list")
    @Operation(summary = "查询区域下的通道")
    @Parameter(name = "civilCode", description = "区域编号")
    public List<CommonGbChannel> getChannelsInRegion(String civilCode) {
        return commonGbChannelService.getChannelsInRegion(civilCode);
    }


    /**
     * 查询分组下的通道
     *
     * @param businessGroupID 业务分组ID
     */
    @DeleteMapping("/region/list")
    @Operation(summary = "查询分组下的通道")
    @Parameter(name = "businessGroupID", description = "业务分组ID")
    public List<CommonGbChannel> getChannelsInBusinessGroup(String businessGroupID) {
        return commonGbChannelService.getChannelsInBusinessGroup(businessGroupID);
    }


    /**
     * 从下级设备中同步通道
     *
     * @param deviceId 设备编号
     */
    @DeleteMapping("/sync/device")
    @Operation(summary = "从下级设备中同步通道")
    @Parameter(name = "deviceId", description = "设备编号")
    @Parameter(name = "syncKeys", description = "选择性同步的字段")
    public boolean syncFromDevice(String deviceId, String[] syncKeys) {
        return commonGbChannelService.SyncChannelFromGb28181Device(deviceId, Lists.newArrayList(syncKeys));
    }
}
