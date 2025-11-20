package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.controller.bean.UpdateChannelParam;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
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

/**
 * 级联平台管理
 */
@Tag(name  = "级联平台管理")
@Slf4j
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private SipConfig sipConfig;

	@Autowired
	private IPlatformService platformService;


    @Operation(summary = "获取国标服务的配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/server_config")
    public JSONObject serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getShowIp());
        result.put("devicePort", sipConfig.getPort());
        result.put("username", sipConfig.getId());
        result.put("password", sipConfig.getPassword());
        return result;
    }

    @Operation(summary = "获取级联服务器信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "平台国标编号", required = true)
    @GetMapping("/info/{id}")
    public Platform getPlatform(@PathVariable String id) {
        Platform parentPlatform = platformService.queryPlatformByServerGBId(id);
        if (parentPlatform != null) {
            return  parentPlatform;
        } else {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未查询到此平台");
        }
    }

    @GetMapping("/query")
    @Operation(summary = "分页查询级联平台", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    public PageInfo<Platform> platforms(int page, int count,
                                        @RequestParam(required = false) String query) {

        PageInfo<Platform> parentPlatformPageInfo = platformService.queryPlatformList(page, count, query);
        if (parentPlatformPageInfo != null && !parentPlatformPageInfo.getList().isEmpty()) {
            for (Platform platform : parentPlatformPageInfo.getList()) {
                platform.setMobilePositionSubscribe(subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId()) != null);
                platform.setCatalogSubscribe(subscribeHolder.getCatalogSubscribe(platform.getServerGBId()) != null);
            }
        }
        return parentPlatformPageInfo;
    }

    @Operation(summary = "添加上级平台信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/add")
    @ResponseBody
    public void add(@RequestBody Platform platform) {

        Assert.notNull(platform.getName(), "平台名称不可为空");
        Assert.notNull(platform.getServerGBId(), "上级平台国标编号不可为空");
        Assert.notNull(platform.getServerIp(), "上级平台IP不可为空");
        Assert.isTrue(platform.getServerPort() > 0 && platform.getServerPort() < 65535, "上级平台端口异常");
        Assert.notNull(platform.getDeviceGBId(), "本平台国标编号不可为空");

        if (ObjectUtils.isEmpty(platform.getServerGBDomain())) {
            platform.setServerGBDomain(platform.getServerGBId().substring(0, 6));
        }

        if (platform.getExpires() <= 0) {
            platform.setExpires(3600);
        }

        if (platform.getKeepTimeout() <= 0) {
            platform.setKeepTimeout(60);
        }

        if (ObjectUtils.isEmpty(platform.getTransport())) {
            platform.setTransport("UDP");
        }

        if (ObjectUtils.isEmpty(platform.getCharacterSet())) {
            platform.setCharacterSet("GB2312");
        }

        Platform parentPlatformOld = platformService.queryPlatformByServerGBId(platform.getServerGBId());
        if (parentPlatformOld != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台 " + platform.getServerGBId() + " 已存在");
        }
        platform.setCreateTime(DateUtil.getNow());
        platform.setUpdateTime(DateUtil.getNow());
        boolean updateResult = platformService.add(platform);

        if (!updateResult) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "更新上级平台信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/update")
    @ResponseBody
    public void updatePlatform(@RequestBody Platform parentPlatform) {

        if (ObjectUtils.isEmpty(parentPlatform.getName())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBId())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBDomain())
                || ObjectUtils.isEmpty(parentPlatform.getServerIp())
                || ObjectUtils.isEmpty(parentPlatform.getServerPort())
                || ObjectUtils.isEmpty(parentPlatform.getDeviceGBId())
                || ObjectUtils.isEmpty(parentPlatform.getExpires())
                || ObjectUtils.isEmpty(parentPlatform.getKeepTimeout())
                || ObjectUtils.isEmpty(parentPlatform.getTransport())
                || ObjectUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        platformService.update(parentPlatform);
    }

    @Operation(summary = "删除上级平台", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "上级平台ID")
    @DeleteMapping("/delete")
    @ResponseBody
    public WVPResult<?> deletePlatform(Integer id) {

        if (log.isDebugEnabled()) {
            log.debug("删除上级平台API调用");
        }
        boolean result = platformService.delete(id);
        if (result) {
            return WVPResult.success();
        }else {
            return  WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "查询上级平台是否存在", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public Boolean exitPlatform(@PathVariable String serverGBId) {
        Platform platform = platformService.queryPlatformByServerGBId(serverGBId);
        return platform != null;
    }

    @Operation(summary = "分页查询级联平台的所有所有通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    @Parameter(name = "platformId", description = "上级平台的数据ID")
    @Parameter(name = "channelType", description = "通道类型， 0：国标设备，1：推流设备，2：拉流代理")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "hasShare", description = "是否已经共享")
    @GetMapping("/channel/list")
    @ResponseBody
    public PageInfo<PlatformChannel> queryChannelList(int page, int count,
                                                      @RequestParam(required = false) Integer platformId,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(required = false) Integer channelType,
                                                      @RequestParam(required = false) Boolean online,
                                                      @RequestParam(required = false) Boolean hasShare) {

        Assert.notNull(platformId, "上级平台的数据ID不可为NULL");
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }

        return platformChannelService.queryChannelList(page, count, query, channelType,  online, platformId, hasShare);
    }

    @Operation(summary = "向上级平台添加国标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/add")
    @ResponseBody
    public void addChannel(@RequestBody UpdateChannelParam param) {

        if (log.isDebugEnabled()) {
            log.debug("给上级平台添加国标通道API调用");
        }
        int result = 0;
        if (param.getChannelIds() == null || param.getChannelIds().isEmpty()) {
            if (param.isAll()) {
                log.info("[国标级联]添加所有通道到上级平台， {}", param.getPlatformId());
                result = platformChannelService.addAllChannel(param.getPlatformId());
            }
        }else {
            result = platformChannelService.addChannels(param.getPlatformId(), param.getChannelIds());
        }
        if (result <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "从上级平台移除国标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @DeleteMapping("/channel/remove")
    @ResponseBody
    public void delChannelForGB(@RequestBody UpdateChannelParam param) {

        if (log.isDebugEnabled()) {
            log.debug("给上级平台删除国标通道API调用");
        }
        int result = 0;
        if (param.getChannelIds() == null || param.getChannelIds().isEmpty()) {
            if (param.isAll()) {
                log.info("[国标级联]移除所有通道，上级平台， {}", param.getPlatformId());
                result = platformChannelService.removeAllChannel(param.getPlatformId());
            }
        }else {
            result = platformChannelService.removeChannels(param.getPlatformId(), param.getChannelIds());
        }
        if (result <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "推送通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "平台ID", required = true)
    @GetMapping("/channel/push")
    @ResponseBody
    public void pushChannel(Integer id) {
        Assert.notNull(id, "平台ID不可为空");
        platformChannelService.pushChannel(id);
    }

    @Operation(summary = "添加通道-通过设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/device/add")
    @ResponseBody
    public void addChannelByDevice(@RequestBody UpdateChannelParam param) {
        Assert.notNull(param.getPlatformId(), "平台ID不可为空");
        Assert.notNull(param.getDeviceIds(), "设备ID不可为空");
        Assert.notEmpty(param.getDeviceIds(), "设备ID不可为空");
        platformChannelService.addChannelByDevice(param.getPlatformId(), param.getDeviceIds());
    }

    @Operation(summary = "移除通道-通过设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/device/remove")
    @ResponseBody
    public void removeChannelByDevice(@RequestBody UpdateChannelParam param) {
        Assert.notNull(param.getPlatformId(), "平台ID不可为空");
        Assert.notNull(param.getDeviceIds(), "设备ID不可为空");
        Assert.notEmpty(param.getDeviceIds(), "设备ID不可为空");
        platformChannelService.removeChannelByDevice(param.getPlatformId(), param.getDeviceIds());
    }

    @Operation(summary = "自定义共享通道信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/custom/update")
    @ResponseBody
    public void updateCustomChannel(@RequestBody PlatformChannel channel) {
        Assert.isTrue(channel.getId() > 0, "共享通道ID必须存在");
        platformChannelService.updateCustomChannel(channel);
    }
}
