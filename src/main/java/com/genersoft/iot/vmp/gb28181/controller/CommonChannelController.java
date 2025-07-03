package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceType;
import com.genersoft.iot.vmp.gb28181.bean.IndustryCodeType;
import com.genersoft.iot.vmp.gb28181.bean.NetworkIdentificationType;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToGroupByGbDeviceParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToGroupParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToRegionByGbDeviceParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToRegionParam;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
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
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
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

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private UserSetting userSetting;


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
    @Parameter(name = "hasRecordPlan", description = "是否已设置录制计划")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @GetMapping("/list")
    public PageInfo<CommonGBChannel> queryList(int page, int count,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean online,
                                                          @RequestParam(required = false) Boolean hasRecordPlan,
                                                          @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryList(page, count, query, online, hasRecordPlan, channelType);
    }

    @Operation(summary = "获取关联行政区划通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @Parameter(name = "civilCode", description = "行政区划")
    @GetMapping("/civilcode/list")
    public PageInfo<CommonGBChannel> queryListByCivilCode(int page, int count,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Integer channelType,
                                               @RequestParam(required = false) String civilCode){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByCivilCode(page, count, query, online, channelType, civilCode);
    }


    @Operation(summary = "存在行政区划但无法挂载的通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @GetMapping("/civilCode/unusual/list")
    public PageInfo<CommonGBChannel> queryListByCivilCodeForUnusual(int page, int count,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean online,
                                                          @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByCivilCodeForUnusual(page, count, query, online, channelType);
    }


    @Operation(summary = "存在父节点编号但无法挂载的通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @GetMapping("/parent/unusual/list")
    public PageInfo<CommonGBChannel> queryListByParentForUnusual(int page, int count,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean online,
                                                          @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByParentForUnusual(page, count, query, online, channelType);
    }

    @Operation(summary = "清除存在行政区划但无法挂载的通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "清理参数， all为true清理所有异常数据。 否则按照传入的设备Id清理", required = true)
    @PostMapping("/civilCode/unusual/clear")
    public void clearChannelCivilCode(@RequestBody ChannelToRegionParam param){
        channelService.clearChannelCivilCode(param.getAll(), param.getChannelIds());
    }

    @Operation(summary = "清除存在分组节点但无法挂载的通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "清理参数， all为true清理所有异常数据。 否则按照传入的设备Id清理", required = true)
    @PostMapping("/parent/unusual/clear")
    public void clearChannelParent(@RequestBody ChannelToRegionParam param){
        channelService.clearChannelParent(param.getAll(), param.getChannelIds());
    }

    @Operation(summary = "获取关联业务分组通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @Parameter(name = "groupDeviceId", description = "业务分组下的父节点ID")
    @GetMapping("/parent/list")
    public PageInfo<CommonGBChannel> queryListByParentId(int page, int count,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Integer channelType,
                                               @RequestParam(required = false) String groupDeviceId){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByParentId(page, count, query, online, channelType, groupDeviceId);
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
    public void addChannelToRegionByGbDevice(@RequestBody ChannelToRegionByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"参数异常");
        Assert.hasLength(param.getCivilCode(),"未添加行政区划");
        channelService.addChannelToRegionByGbDevice(param.getCivilCode(), param.getDeviceIds());
    }

    @Operation(summary = "通道删除行政区划-根据国标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/device/delete")
    public void deleteChannelToRegionByGbDevice(@RequestBody ChannelToRegionByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"参数异常");
        channelService.deleteChannelToRegionByGbDevice(param.getDeviceIds());
    }

    @Operation(summary = "通道设置业务分组", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/add")
    public void addChannelToGroup(@RequestBody ChannelToGroupParam param){
        Assert.notEmpty(param.getChannelIds(),"通道ID不可为空");
        Assert.hasLength(param.getParentId(),"未添加上级分组编号");
        Assert.hasLength(param.getBusinessGroup(),"未添加业务分组");
        channelService.addChannelToGroup(param.getParentId(), param.getBusinessGroup(), param.getChannelIds());
    }

    @Operation(summary = "通道删除业务分组", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/delete")
    public void deleteChannelToGroup(@RequestBody ChannelToGroupParam param){
        Assert.isTrue(!param.getChannelIds().isEmpty()
                || (!ObjectUtils.isEmpty(param.getParentId()) && !ObjectUtils.isEmpty(param.getBusinessGroup())),
                "参数异常");
        channelService.deleteChannelToGroup(param.getParentId(), param.getBusinessGroup(), param.getChannelIds());
    }

    @Operation(summary = "通道设置业务分组-根据国标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/device/add")
    public void addChannelToGroupByGbDevice(@RequestBody ChannelToGroupByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"参数异常");
        Assert.hasLength(param.getParentId(),"未添加上级分组编号");
        Assert.hasLength(param.getBusinessGroup(),"未添加业务分组");
        channelService.addChannelToGroupByGbDevice(param.getParentId(), param.getBusinessGroup(), param.getDeviceIds());
    }

    @Operation(summary = "通道删除业务分组-根据国标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/device/delete")
    public void deleteChannelToGroupByGbDevice(@RequestBody ChannelToGroupByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"参数异常");
        channelService.deleteChannelToGroupByGbDevice(param.getDeviceIds());
    }

    @Operation(summary = "播放通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/play")
    public DeferredResult<WVPResult<StreamContent>> deleteChannelToGroupByGbDevice(HttpServletRequest request,  Integer channelId){
        Assert.notNull(channelId,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                WVPResult<StreamContent> wvpResult = WVPResult.success();
                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo=streamInfo.clone();//深拷贝
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }

                result.setResult(wvpResult);
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };
        channelPlayService.play(channel, null, userSetting.getRecordSip(), callback);
        return result;
    }
}
