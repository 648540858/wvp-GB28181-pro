package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToGroupByGbDeviceParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToGroupParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToRegionByGbDeviceParam;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelToRegionParam;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelFrontEndService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Tag(name  = "全局通道管理")
@RestController
@Slf4j
@RequestMapping(value = "/api/common/channel")
public class ChannelController {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private IGbChannelFrontEndService channelFrontEndService;

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
    public DeferredResult<WVPResult<StreamContent>> play(HttpServletRequest request,  Integer channelId){
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

    @Operation(summary = "停止播放通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/play/stop")
    public void stopPlay(Integer channelId){
        Assert.notNull(channelId,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.stopPlay(channel, channel.getStreamId());
    }

    @Operation(summary = "录像查询", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "startTime", description = "开始时间", required = true)
    @Parameter(name = "endTime", description = "结束时间", required = true)
    @GetMapping("/playback/query")
    public DeferredResult<WVPResult<List<CommonRecordInfo>>> queryRecord(Integer channelId, String startTime, String endTime){

        DeferredResult<WVPResult<List<CommonRecordInfo>>> result = new DeferredResult<>(Long.valueOf(userSetting.getRecordInfoTimeout()), TimeUnit.MILLISECONDS);
        if (!DateUtil.verification(startTime, DateUtil.formatter)){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTime格式为" + DateUtil.PATTERN);
        }
        if (!DateUtil.verification(endTime, DateUtil.formatter)){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTime格式为" + DateUtil.PATTERN);
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelPlayService.queryRecord(channel, startTime, endTime, (code, msg, data) -> {
            WVPResult<List<CommonRecordInfo>> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        });
        result.onTimeout(()->{
            WVPResult<List<CommonRecordInfo>> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("timeout");
            result.setResult(wvpResult);
        });
        return result;
    }

    @Operation(summary = "录像回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "startTime", description = "开始时间", required = true)
    @Parameter(name = "endTime", description = "结束时间", required = true)
    @GetMapping("/playback")
    public DeferredResult<WVPResult<StreamContent>> playback(HttpServletRequest request, Integer channelId, String startTime, String endTime){
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
        channelPlayService.playback(channel, DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime),
                DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime), callback);
        return result;
    }

    @Operation(summary = "停止录像回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @GetMapping("/playback/stop")
    public void stopPlayback(Integer channelId, String stream){
        Assert.notNull(channelId,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.stopPlayback(channel, stream);
    }

    @Operation(summary = "暂停录像回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @GetMapping("/playback/pause")
    public void pausePlayback(Integer channelId, String stream){
        Assert.notNull(channelId,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.playbackPause(channel, stream);
    }

    @Operation(summary = "恢复录像回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @GetMapping("/playback/resume")
    public void resumePlayback(Integer channelId, String stream){
        Assert.notNull(channelId,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.playbackResume(channel, stream);
    }

    @Operation(summary = "拖动录像回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @Parameter(name = "seekTime", description = "将要播放的时间", required = true)
    @GetMapping("/playback/seek")
    public void seekPlayback(Integer channelId, String stream, Long seekTime){
        Assert.notNull(channelId,"参数异常");
        Assert.notNull(seekTime,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.playbackSeek(channel, stream, seekTime);
    }

    @Operation(summary = "拖动录像回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @Parameter(name = "speed", description = "倍速", required = true)
    @GetMapping("/playback/speed")
    public void seekPlayback(Integer channelId, String stream, Double speed){
        Assert.notNull(channelId,"参数异常");
        Assert.notNull(speed,"参数异常");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.playbackSpeed(channel, stream, speed);
    }


    @Operation(summary = "云台控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
    @Parameter(name = "horizonSpeed", description = "水平速度(0-255)", required = true)
    @Parameter(name = "verticalSpeed", description = "垂直速度(0-255)", required = true)
    @Parameter(name = "zoomSpeed", description = "缩放速度(0-15)", required = true)
    @GetMapping("/ptz")
    public void ptz(Integer channelId, String command, Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed){

        if (log.isDebugEnabled()) {
            log.debug(String.format("设备云台控制 API调用，channelId：%s ，command：%s ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",channelId, command, horizonSpeed, verticalSpeed, zoomSpeed));
        }

        if (horizonSpeed == null) {
            horizonSpeed = 100;
        }else if (horizonSpeed < 0 || horizonSpeed > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "horizonSpeed 为 0-255的数字");
        }
        if (verticalSpeed == null) {
            verticalSpeed = 100;
        }else if (verticalSpeed < 0 || verticalSpeed > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "verticalSpeed 为 0-255的数字");
        }
        if (zoomSpeed == null) {
            zoomSpeed = 16;
        }else if (zoomSpeed < 0 || zoomSpeed > 15) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "zoomSpeed 为 0-15的数字");
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.ptz(channel, command, horizonSpeed, verticalSpeed, zoomSpeed);
    }


    @Operation(summary = "光圈控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: in, out, stop", required = true)
    @Parameter(name = "speed", description = "光圈速度(0-255)", required = true)
    @GetMapping("/fi/iris")
    public void iris(Integer channelId, String command, Integer speed){

        if (log.isDebugEnabled()) {
            log.debug("设备光圈控制 API调用，channelId：{} ，command：{} ，speed：{} ",channelId, command, speed);
        }

        if (speed == null) {
            speed = 100;
        }else if (speed < 0 || speed > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed 为 0-255的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.iris(channel, command, speed);
    }

    @Operation(summary = "聚焦控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: near, far, stop", required = true)
    @Parameter(name = "speed", description = "聚焦速度(0-255)", required = true)
    @GetMapping("/fi/focus")
    public void focus(Integer channelId, String command, Integer speed){

        if (log.isDebugEnabled()) {
            log.debug("设备聚焦控制 API调用，channelId：{} ，command：{} ，speed：{} ",channelId, command, speed);
        }

        if (speed == null) {
            speed = 100;
        }else if (speed < 0 || speed > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed 为 0-255的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.focus(channel, command, speed);
    }

    @Operation(summary = "查询预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @GetMapping("/preset/query")
    public DeferredResult<WVPResult<Object>> queryPreset(Integer channelId) {
        if (log.isDebugEnabled()) {
            log.debug("设备预置位查询API调用");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        DeferredResult<WVPResult<Object>> deferredResult = new DeferredResult<> (3 * 1000L);
        channelFrontEndService.queryPreset(channel, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });

        deferredResult.onTimeout(()->{
            log.warn("[获取设备预置位] 超时, {}", channelId);
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
        });
        return deferredResult;
    }

    @Operation(summary = "预置位指令-设置预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
    @GetMapping("/preset/add")
    public void addPreset(Integer channelId, Integer presetId) {
        if (presetId == null || presetId < 1 || presetId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.addPreset(channel, presetId);
    }

    @Operation(summary = "预置位指令-调用预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
    @GetMapping("/preset/call")
    public void callPreset(Integer channelId, Integer presetId) {
        if (presetId == null || presetId < 1 || presetId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.callPreset(channel, presetId);
    }

    @Operation(summary = "预置位指令-删除预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
    @GetMapping("/preset/delete")
    public void deletePreset(Integer channelId, Integer presetId) {
        if (presetId == null || presetId < 1 || presetId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.deletePreset(channel, presetId);
    }

    @Operation(summary = "巡航指令-加入巡航点", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "cruiseId", description = "巡航组号(0-255)", required = true)
    @Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
    @GetMapping("/cruise/point/add")
    public void addCruisePoint(Integer channelId, Integer cruiseId, Integer presetId) {
        if (presetId == null || cruiseId == null || presetId < 1 || presetId > 255 || cruiseId < 0 || cruiseId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.addCruisePoint(channel, cruiseId, presetId);
    }

    @Operation(summary = "巡航指令-删除一个巡航点", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "cruiseId", description = "巡航组号(1-255)", required = true)
    @Parameter(name = "presetId", description = "预置位编号(0-255, 为0时删除整个巡航)", required = true)
    @GetMapping("/cruise/point/delete")
    public void deleteCruisePoint(Integer channelId, Integer cruiseId, Integer presetId) {
        if (presetId == null || presetId < 0 || presetId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为0-255之间的数字, 为0时删除整个巡航");
        }
        if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.deleteCruisePoint(channel, cruiseId, presetId);
    }

    @Operation(summary = "巡航指令-设置巡航速度", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "cruiseId", description = "巡航组号(0-255)", required = true)
    @Parameter(name = "speed", description = "巡航速度(1-4095)", required = true)
    @GetMapping("/cruise/speed")
    public void setCruiseSpeed(Integer channelId, Integer cruiseId, Integer speed) {
        if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
        }
        if (speed == null || speed < 1 || speed > 4095) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航速度必须为1-4095之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.setCruiseSpeed(channel, cruiseId, speed);
    }

    @Operation(summary = "巡航指令-设置巡航停留时间", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "cruiseId", description = "巡航组号", required = true)
    @Parameter(name = "time", description = "巡航停留时间(1-4095)", required = true)
    @GetMapping("/cruise/time")
    public void setCruiseTime(Integer channelId, Integer cruiseId, Integer time) {
        if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
        }
        if (time == null || time < 1 || time > 4095) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航停留时间必须为1-4095之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.setCruiseTime(channel, cruiseId, time);
    }

    @Operation(summary = "巡航指令-开始巡航", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "cruiseId", description = "巡航组号)", required = true)
    @GetMapping("/cruise/start")
    public void startCruise(Integer channelId, Integer cruiseId) {
        if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.startCruise(channel, cruiseId);
    }

    @Operation(summary = "巡航指令-停止巡航", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "cruiseId", description = "巡航组号", required = true)
    @GetMapping("/cruise/stop")
    public void stopCruise(Integer channelId, Integer cruiseId) {
        if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.stopCruise(channel, cruiseId);
    }

    @Operation(summary = "扫描指令-开始自动扫描", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
    @GetMapping("/scan/start")
    public void startScan(Integer channelId, Integer scanId) {
        if (scanId == null || scanId < 0 || scanId > 255 ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.startScan(channel, scanId);
    }

    @Operation(summary = "扫描指令-停止自动扫描", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
    @GetMapping("/scan/stop")
    public void stopScan(Integer channelId, Integer scanId) {
        if (scanId == null || scanId < 0 || scanId > 255 ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.stopScan(channel, scanId);
    }

    @Operation(summary = "扫描指令-设置自动扫描左边界", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
    @GetMapping("/scan/set/left")
    public void setScanLeft(Integer channelId, Integer scanId) {
        if (scanId == null || scanId < 0 || scanId > 255 ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.setScanLeft(channel, scanId);
    }

    @Operation(summary = "扫描指令-设置自动扫描右边界", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
    @GetMapping("/scan/set/right")
    public void setScanRight(Integer channelId, Integer scanId) {
        if (scanId == null || scanId < 0 || scanId > 255 ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.setScanRight(channel, scanId);
    }


    @Operation(summary = "扫描指令-设置自动扫描速度", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
    @Parameter(name = "speed", description = "自动扫描速度(1-4095)", required = true)
    @GetMapping("/scan/set/speed")
    public void setScanSpeed(Integer channelId, Integer scanId, Integer speed) {
        if (scanId == null || scanId < 0 || scanId > 255 ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
        }
        if (speed == null || speed < 1 || speed > 4095) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "自动扫描速度必须为1-4095之间的数字");
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.setScanSpeed(channel, scanId, speed);
    }


    @Operation(summary = "辅助开关控制指令-雨刷控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: on, off", required = true)
    @GetMapping("/wiper")
    public void wiper(Integer channelId, String command){

        if (log.isDebugEnabled()) {
            log.debug("辅助开关控制指令-雨刷控制 API调用，channelId：{} ，command：{}",channelId, command);
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.wiper(channel, command);
    }

    @Operation(summary = "辅助开关控制指令", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: on, off", required = true)
    @Parameter(name = "switchId", description = "开关编号", required = true)
    @GetMapping("/auxiliary")
    public void auxiliarySwitch(Integer channelId, String command, Integer switchId){

        if (log.isDebugEnabled()) {
            log.debug("辅助开关控制指令-雨刷控制 API调用，channelId：{} ，command：{}, switchId: {}", channelId, command, switchId);
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "通道不存在");

        channelFrontEndService.auxiliarySwitch(channel, command, switchId);
    }

    @Operation(summary = "为地图获取通道列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "hasRecordPlan", description = "是否已设置录制计划")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @Parameter(name = "geoCoordSys", description = "地理坐标系， WGS84/GCJ02")
    @GetMapping("/map/list")
    public List<CommonGBChannel> queryListForMap(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) Boolean hasRecordPlan,
            @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListForMap(query, online, hasRecordPlan, channelType);
    }
}
