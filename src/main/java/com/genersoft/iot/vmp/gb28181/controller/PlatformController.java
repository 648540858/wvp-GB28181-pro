package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import com.genersoft.iot.vmp.gb28181.controller.bean.UpdateChannelParam;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
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

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 级联平台管理
 */
@Tag(name  = "级联平台管理")
@Slf4j
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private SipConfig sipConfig;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private IDeviceChannelService deviceChannelService;

    /**
     * 获取国标服务的配置
     *
     * @return
     */
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

    /**
     * 获取级联服务器信息
     *
     * @return
     */
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

    /**
     * 分页查询级联平台
     *
     * @param page  当前页
     * @param count 每页条数
     * @return
     */
    @GetMapping("/query/{count}/{page}")
    @Operation(summary = "分页查询级联平台", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    public PageInfo<Platform> platforms(@PathVariable int page, @PathVariable int count) {

        PageInfo<Platform> parentPlatformPageInfo = platformService.queryPlatformList(page, count);
        if (parentPlatformPageInfo.getList().size() > 0) {
            for (Platform platform : parentPlatformPageInfo.getList()) {
                platform.setMobilePositionSubscribe(subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId()) != null);
                platform.setCatalogSubscribe(subscribeHolder.getCatalogSubscribe(platform.getServerGBId()) != null);
            }
        }
        return parentPlatformPageInfo;
    }

    /**
     * 添加上级平台信息
     *
     * @param platform
     * @return
     */
    @Operation(summary = "添加上级平台信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/add")
    @ResponseBody
    public void addPlatform(@RequestBody Platform platform) {

        if (log.isDebugEnabled()) {
            log.debug("保存上级平台信息API调用");
        }
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

    /**
     * 保存上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @Operation(summary = "保存上级平台信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/update")
    @ResponseBody
    public void updatePlatform(@RequestBody Platform parentPlatform) {

        if (log.isDebugEnabled()) {
            log.debug("保存上级平台信息API调用");
        }
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

    /**
     * 删除上级平台
     *
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @Operation(summary = "删除上级平台", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @DeleteMapping("/delete/{serverGBId}")
    @ResponseBody
    public void deletePlatform(@PathVariable String serverGBId) {

        if (log.isDebugEnabled()) {
            log.debug("删除上级平台API调用");
        }
        if (ObjectUtils.isEmpty(serverGBId)
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        Platform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        PlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(serverGBId);
        if (parentPlatform == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
        if (parentPlatformCatch == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
        parentPlatform.setEnable(false);
        storager.updateParentPlatform(parentPlatform);
        // 发送离线消息,无论是否成功都删除缓存
        try {
            commanderForPlatform.unregister(parentPlatform, parentPlatformCatch.getSipTransactionInfo(), (event -> {
                // 清空redis缓存
                redisCatchStorage.delPlatformCatchInfo(parentPlatform.getServerGBId());
                redisCatchStorage.delPlatformKeepalive(parentPlatform.getServerGBId());
                redisCatchStorage.delPlatformRegister(parentPlatform.getServerGBId());
            }), (event -> {
                // 清空redis缓存
                redisCatchStorage.delPlatformCatchInfo(parentPlatform.getServerGBId());
                redisCatchStorage.delPlatformKeepalive(parentPlatform.getServerGBId());
                redisCatchStorage.delPlatformRegister(parentPlatform.getServerGBId());
            }));
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
        }

        boolean deleteResult = storager.deleteParentPlatform(parentPlatform);
//        storager.delCatalogByPlatformId(parentPlatform.getServerGBId());
//        storager.delRelationByPlatformId(parentPlatform.getServerGBId());
        // 停止发送位置订阅定时任务
        String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetting.getServerId() +  "_MobilePosition_" + parentPlatform.getServerGBId();
        dynamicTask.stop(key);
        // 删除缓存的订阅信息
        subscribeHolder.removeAllSubscribe(parentPlatform.getServerGBId());
        if (!deleteResult) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * 查询上级平台是否存在
     *
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @Operation(summary = "查询上级平台是否存在", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public Boolean exitPlatform(@PathVariable String serverGBId) {

        Platform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        return parentPlatform != null;
    }

    /**
     * 分页查询级联平台的所有所有通道
     *
     * @param page        当前页
     * @param count       每页条数
     * @param platformId  上级平台ID
     * @param query       查询内容
     * @param online      是否在线
     * @param channelType 通道类型
     * @return
     */
    @Operation(summary = "分页查询级联平台的所有所有通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    @Parameter(name = "platformId", description = "上级平台的数据ID")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "hasShare", description = "是否已经共享")
    @GetMapping("/channel/list")
    @ResponseBody
    public PageInfo<CommonGBChannel> channelList(int page, int count,
                                                 @RequestParam(required = false) Integer platformId,
                                                 @RequestParam(required = false) String query,
                                                 @RequestParam(required = false) Boolean online,
                                                 @RequestParam(required = false) Boolean hasShare) {

        Assert.notNull(platformId, "上级平台的数据ID不可为NULL");
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }

        return platformChannelService.queryChannelList(page, count, query, online, platformId, hasShare);
    }

    /**
     * 向上级平台添加国标通道
     *
     * @param param 通道关联参数
     * @return
     */
    @Operation(summary = "向上级平台添加国标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/update_channel_for_gb")
    @ResponseBody
    public void updateChannelForGB(@RequestBody UpdateChannelParam param) {

        if (log.isDebugEnabled()) {
            log.debug("给上级平台添加国标通道API调用");
        }
        int result = 0;
        if (param.getChannelReduces() == null || param.getChannelReduces().size() == 0) {
            if (param.isAll()) {
                log.info("[国标级联]添加所有通道到上级平台， {}", param.getPlatformId());
                List<ChannelReduce> allChannelForDevice = deviceChannelService.queryAllChannelList(param.getPlatformId());
                result = platformChannelService.updateChannelForGB(param.getPlatformId(), allChannelForDevice, param.getCatalogId());
            }
        }else {
            result = platformChannelService.updateChannelForGB(param.getPlatformId(), param.getChannelReduces(), param.getCatalogId());
        }
        if (result <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * 从上级平台移除国标通道
     *
     * @param param 通道关联参数
     * @return
     */
    @Operation(summary = "从上级平台移除国标通道", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @DeleteMapping("/del_channel_for_gb")
    @ResponseBody
    public void delChannelForGB(@RequestBody UpdateChannelParam param) {

        if (log.isDebugEnabled()) {
            log.debug("给上级平台删除国标通道API调用");
        }
        int result = 0;
        if (param.getChannelReduces() == null || param.getChannelReduces().size() == 0) {
            if (param.isAll()) {
                log.info("[国标级联]移除所有通道，上级平台， {}", param.getPlatformId());
                result = platformChannelService.delAllChannelForGB(param.getPlatformId(), param.getCatalogId());
            }
        }else {
            result = storager.delChannelForGB(param.getPlatformId(), param.getChannelReduces());
        }
        if (result <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * 删除关联
     *
     * @param platformCatalog 关联的信息
     * @return
     */
    @Operation(summary = "删除关联", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @DeleteMapping("/catalog/relation/del")
    @ResponseBody
    public void delRelation(@RequestBody PlatformCatalog platformCatalog) {

        if (log.isDebugEnabled()) {
            log.debug("删除关联,{}", JSON.toJSONString(platformCatalog));
        }
//        int delResult = storager.delRelation(platformCatalog);
//
//        if (delResult <= 0) {
//            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入数据库失败");
//        }
    }


}
