package com.genersoft.iot.vmp.vmanager.channel;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "通用国标通道")

@RestController
@RequestMapping("/api/channel")
public class CommonChannelController {

    private final static Logger logger = LoggerFactory.getLogger(CommonChannelController.class);

    @Autowired
    private ICommonGbChannelService commonGbChannelService;


    @Operation(summary = "查询区域下的通道")
    @Parameter(name = "regionDeviceId", description = "区域的编号", required = true)
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @GetMapping("/region/list")
    public PageInfo<CommonGbChannel> getChannelsInRegion(
            @RequestParam(required = true) String regionDeviceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ) {
        return commonGbChannelService.getChannelsInRegion(regionDeviceId, query, page, count);
    }

    @Operation(summary = "查询分组下的通道")
    @Parameter(name = "groupDeviceId", description = "分组的编号", required = true)
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/group/list")
    public PageInfo<CommonGbChannel> queryChannelListInGroup(
            @RequestParam(required = true) String groupDeviceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count ){

        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        assert !ObjectUtils.isEmpty(groupDeviceId);
        return commonGbChannelService.queryChannelListInGroup(groupDeviceId, query, page, count);
    }

    /**
     * 从下级设备中同步通道
     *
     * @param deviceId 设备编号
     */
    @GetMapping("/sync/device")
    @Operation(summary = "从下级设备中同步通道")
    @Parameter(name = "deviceId", description = "设备编号")
    @Parameter(name = "syncKeys", description = "选择性同步的字段")
    public boolean syncFromDevice(String deviceId, String[] syncKeys,
                                  @RequestParam(required = false) Boolean syncGroup,
                                  @RequestParam(required = false) Boolean syncRegion) {
        return commonGbChannelService.syncChannelFromGb28181Device(deviceId, Lists.newArrayList(syncKeys), syncGroup, syncRegion);
    }


    @Operation(summary = "分页查询通道")
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/list")
    public PageInfo<CommonGbChannel> queryChannelList(
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count ){

        return commonGbChannelService.queryChannelList(query, page, count);
    }

    @Operation(summary = "更新通道")
    @Parameter(name = "CommonGbChannel", description = "commonGbChannel", required = true)
    @ResponseBody
    @GetMapping("/update")
    public void update(
            @RequestParam(required = false) CommonGbChannel commonGbChannel
    ){
        commonGbChannelService.update(commonGbChannel);
    }

}
