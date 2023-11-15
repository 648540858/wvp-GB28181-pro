package com.genersoft.iot.vmp.vmanager.channel;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToGroup;
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
    @Parameter(name = "groupDeviceId", description = "分组的编号", required = false)
    @Parameter(name = "regionDeviceId", description = "区域的编号", required = false)
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "inGroup", description = "是否已经在分组下了", required = false)
    @Parameter(name = "inRegion", description = "是否已经在地区下了", required = false)
    @Parameter(name = "type", description = "通道类型", required = false)
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/list")
    public PageInfo<CommonGbChannel> queryChannelListInGroup(
            @RequestParam(required = false) String groupDeviceId,
            @RequestParam(required = false) String regionDeviceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean inGroup,
            @RequestParam(required = false) Boolean inRegion,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count ){

        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (groupDeviceId != null && ObjectUtils.isEmpty(groupDeviceId.trim())) {
            groupDeviceId = null;
        }
        if (regionDeviceId != null && ObjectUtils.isEmpty(regionDeviceId.trim())) {
            regionDeviceId = null;
        }
        if (type != null && ObjectUtils.isEmpty(type.trim())) {
            type = null;
        }
        assert !ObjectUtils.isEmpty(groupDeviceId);
        return commonGbChannelService.queryChannelListInGroup(page, count, query, groupDeviceId, regionDeviceId,
                inGroup, inRegion, type);
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


//    @Operation(summary = "分页查询通道")
//    @Parameter(name = "query", description = "要搜索的内容", required = false)
//    @Parameter(name = "page", description = "当前页", required = true)
//    @Parameter(name = "count", description = "每页查询数量", required = true)
//    @ResponseBody
//    @GetMapping("/list")
//    public PageInfo<CommonGbChannel> queryChannelList(
//            @RequestParam(required = false) String query,
//            @RequestParam(required = true) int page,
//            @RequestParam(required = true) int count ){
//
//        return commonGbChannelService.queryChannelList(query, page, count);
//    }

    @Operation(summary = "更新通道")
    @Parameter(name = "CommonGbChannel", description = "commonGbChannel", required = true)
    @ResponseBody
    @GetMapping("/update")
    public void update(
            @RequestParam(required = false) CommonGbChannel commonGbChannel
    ){
        commonGbChannelService.update(commonGbChannel);
    }

    @Operation(summary = "获取一个随机的可用国标编号")
    @Parameter(name = "type", description = "类型： " +
            "CIVIL_CODE_PROVINCE 省级编号 " +
            "CIVIL_CODE_CIT 市级编号" +
            "CIVIL_CODE_GRASS_ROOTS 区级编号" +
            "CIVIL_CODE_GRASS_ROOTS 基层接入单位编号 " +
            "BUSINESS_GROUP 业务分组 " +
            "VIRTUAL_ORGANIZATION 虚拟组织 ", required = true)
    @ResponseBody
    @GetMapping("/code/random")
    public String getRandomCode(
            @RequestParam(required = true) Gb28181CodeType type
    ){
        return commonGbChannelService.getRandomCode(type);
    }

    @Operation(summary = "获取行业编码列表")
    @ResponseBody
    @GetMapping("/industry/list")
    public List<IndustryCodeType> getIndustryCodeList(){
        return commonGbChannelService.getIndustryCodeList();
    }

    @Operation(summary = "获取编码列表")
    @ResponseBody
    @GetMapping("/type/list")
    public List<DeviceType> getDeviceTypeList(){
        return commonGbChannelService.getDeviceTypeList();
    }

    @Operation(summary = "获取编码列表")
    @ResponseBody
    @GetMapping("/network/identification/list")
    public List<NetworkIdentificationType> getNetworkIdentificationTypeList(){
        return commonGbChannelService.getNetworkIdentificationTypeList();
    }

    @Operation(summary = "为通道添加分组")
    @ResponseBody
    @PostMapping("/group/update")
    public void updateChannelToGroup(@RequestBody UpdateCommonChannelToGroup params){
        assert params.getCommonGbBusinessGroupID() != null;
        assert !params.getCommonGbIds().isEmpty();
        commonGbChannelService.updateChannelToGroup(params);
    }

    @Operation(summary = "从分组中移除通道")
    @ResponseBody
    @PostMapping("/group/remove")
    public void removeFromGroup(@RequestBody UpdateCommonChannelToGroup params){
        assert params.getCommonGbBusinessGroupID() != null || !params.getCommonGbIds().isEmpty();
        commonGbChannelService.removeFromGroup(params);
    }




}
