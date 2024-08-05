package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceType;
import com.genersoft.iot.vmp.gb28181.bean.IndustryCodeType;
import com.genersoft.iot.vmp.gb28181.bean.NetworkIdentificationType;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToRegionBYGbDeviceParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToRegionParam;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name  = "全局通道管理")
@RestController
@Slf4j
@RequestMapping(value = "/api/common/channel")
public class CommonChannelController {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IMediaServerService mediaServerService;


    @Operation(summary = "查询通道信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "通道的数据库自增Id", required = true)
    @GetMapping(value = "/one")
    public CommonGBChannel getOne(int id){
        return channelService.getOne(id);
    }

    @Operation(summary = "获取行业编码列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/industry/list")
    public List<IndustryCodeType> getIndustryCodeList(){
        return channelService.getIndustryCodeList();
    }

    @Operation(summary = "获取编码列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/type/list")
    public List<DeviceType> getDeviceTypeList(){
        return channelService.getDeviceTypeList();
    }

    @Operation(summary = "获取编码列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/network/identification/list")
    public List<NetworkIdentificationType> getNetworkIdentificationTypeList(){
        return channelService.getNetworkIdentificationTypeList();
    }

    @Operation(summary = "更新通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/update")
    public void update(@RequestBody CommonGBChannel channel){
        channelService.update(channel);
    }

    @Operation(summary = "重置国标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/reset")
    public void reset(Integer id){
        channelService.reset(id);
    }

    @Operation(summary = "增加通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/add")
    public CommonGBChannel add(@RequestBody CommonGBChannel channel){
        channelService.add(channel);
        return channel;
    }

    @Operation(summary = "获取通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "hasCivilCode", description = "是否分配行政区划")
    @GetMapping("/list")
    public PageInfo<CommonGBChannel> queryList(int page, int count,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Boolean hasCivilCode,
                                               @RequestParam(required = false) Boolean hasGroup){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryList(page, count, query, online, hasCivilCode, hasGroup);
    }

    @Operation(summary = "通道设置行政区划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/add")
    public void addChannelToRegion(@RequestBody ChannelToRegionParam param){
        Assert.notEmpty(param.getChannelIds(),"通道ID不可为空");
        Assert.hasLength(param.getCivilCode(),"未添加行政区划");
        channelService.addChannelToRegion(param.getCivilCode(), param.getChannelIds());
    }

    @Operation(summary = "通道删除行政区划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/delete")
    public void deleteChannelToRegion(@RequestBody ChannelToRegionParam param){
        Assert.isTrue(!param.getChannelIds().isEmpty() || !ObjectUtils.isEmpty(param.getCivilCode()),"参数异常");
        channelService.deleteChannelToRegion(param.getCivilCode(), param.getChannelIds());
    }

    @Operation(summary = "通道设置行政区划-根据国标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/device/add")
    public void addChannelToRegionBYGbDevice(@RequestBody ChannelToRegionBYGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"参数异常");
        Assert.hasLength(param.getCivilCode(),"未添加行政区划");
        channelService.addChannelToRegionBYGbDevice(param.getCivilCode(), param.getDeviceIds());
    }

    @Operation(summary = "通道删除行政区划-根据国标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/device/delete")
    public void deleteChannelToRegionBYGbDevice(@RequestBody ChannelToRegionBYGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"参数异常");
        channelService.deleteChannelToRegionBYGbDevice(param.getDeviceIds());
    }
}
