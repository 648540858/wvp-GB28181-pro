package com.genersoft.iot.vmp.vmanager.gb28181.platform;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.UpdateChannelParam;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import com.genersoft.iot.vmp.conf.SipConfig;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 级联平台管理
 */
@Tag(name  = "级联平台管理")

@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final static Logger logger = LoggerFactory.getLogger(PlatformController.class);

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

	@Autowired
	private IGbStreamService gbStreamService;

    /**
     * 获取国标服务的配置
     *
     * @return
     */
    @Operation(summary = "获取国标服务的配置")
    @GetMapping("/server_config")
    public JSONObject serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getIp());
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
    @Operation(summary = "获取级联服务器信息")
    @Parameter(name = "id", description = "平台国标编号", required = true)
    @GetMapping("/info/{id}")
    public ParentPlatform getPlatform(@PathVariable String id) {
        ParentPlatform parentPlatform = platformService.queryPlatformByServerGBId(id);
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
    @Operation(summary = "分页查询级联平台")
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    public PageInfo<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count) {

        PageInfo<ParentPlatform> parentPlatformPageInfo = platformService.queryParentPlatformList(page, count);
        if (parentPlatformPageInfo.getList().size() > 0) {
            for (ParentPlatform platform : parentPlatformPageInfo.getList()) {
                platform.setMobilePositionSubscribe(subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId()) != null);
                platform.setCatalogSubscribe(subscribeHolder.getCatalogSubscribe(platform.getServerGBId()) != null);
            }
        }
        return parentPlatformPageInfo;
    }

    /**
     * 添加上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @Operation(summary = "添加上级平台信息")
    @PostMapping("/add")
    @ResponseBody
    public void addPlatform(@RequestBody ParentPlatform parentPlatform) {

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        if (ObjectUtils.isEmpty(parentPlatform.getName())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBId())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBDomain())
                || ObjectUtils.isEmpty(parentPlatform.getServerIP())
                || ObjectUtils.isEmpty(parentPlatform.getServerPort())
                || ObjectUtils.isEmpty(parentPlatform.getDeviceGBId())
                || ObjectUtils.isEmpty(parentPlatform.getExpires())
                || ObjectUtils.isEmpty(parentPlatform.getKeepTimeout())
                || ObjectUtils.isEmpty(parentPlatform.getTransport())
                || ObjectUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        if (parentPlatform.getServerPort() < 0 || parentPlatform.getServerPort() > 65535) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "error severPort");
        }


        ParentPlatform parentPlatformOld = storager.queryParentPlatByServerGBId(parentPlatform.getServerGBId());
        if (parentPlatformOld != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台 " + parentPlatform.getServerGBId() + " 已存在");
        }
        parentPlatform.setCreateTime(DateUtil.getNow());
        parentPlatform.setUpdateTime(DateUtil.getNow());
        boolean updateResult = platformService.add(parentPlatform);

        if (!updateResult) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"写入数据库失败");
        }
    }

    /**
     * 保存上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @Operation(summary = "保存上级平台信息")
    @PostMapping("/save")
    @ResponseBody
    public void savePlatform(@RequestBody ParentPlatform parentPlatform) {

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        if (ObjectUtils.isEmpty(parentPlatform.getName())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBId())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBDomain())
                || ObjectUtils.isEmpty(parentPlatform.getServerIP())
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
    @Operation(summary = "删除上级平台")
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @DeleteMapping("/delete/{serverGBId}")
    @ResponseBody
    public void deletePlatform(@PathVariable String serverGBId) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除上级平台API调用");
        }
        if (ObjectUtils.isEmpty(serverGBId)
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(serverGBId);
        if (parentPlatform == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
        if (parentPlatformCatch == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
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
            logger.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
        }

        boolean deleteResult = storager.deleteParentPlatform(parentPlatform);
        storager.delCatalogByPlatformId(parentPlatform.getServerGBId());
        storager.delRelationByPlatformId(parentPlatform.getServerGBId());
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
    @Operation(summary = "查询上级平台是否存在")
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public Boolean exitPlatform(@PathVariable String serverGBId) {

        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
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
    @Operation(summary = "查询上级平台是否存在")
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    @Parameter(name = "platformId", description = "上级平台的国标编号")
    @Parameter(name = "catalogId", description = "目录ID")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @Parameter(name = "channelType", description = "通道类型")
    @GetMapping("/channel_list")
    @ResponseBody
    public PageInfo<ChannelReduce> channelList(int page, int count,
                                               @RequestParam(required = false) String platformId,
                                               @RequestParam(required = false) String catalogId,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Boolean channelType) {

        if (ObjectUtils.isEmpty(platformId)) {
            platformId = null;
        }
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(platformId) || ObjectUtils.isEmpty(catalogId)) {
            catalogId = null;
        }
        PageInfo<ChannelReduce> channelReduces = storager.queryAllChannelList(page, count, query, online, channelType, platformId, catalogId);

        return channelReduces;
    }

    /**
     * 向上级平台添加国标通道
     *
     * @param param 通道关联参数
     * @return
     */
    @Operation(summary = "向上级平台添加国标通道")
    @PostMapping("/update_channel_for_gb")
    @ResponseBody
    public void updateChannelForGB(@RequestBody UpdateChannelParam param) {

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台添加国标通道API调用");
        }
        int result = 0;
        if (param.getChannelReduces() == null || param.getChannelReduces().size() == 0) {
            if (param.isAll()) {
                logger.info("[国标级联]添加所有通道到上级平台， {}", param.getPlatformId());
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
    @Operation(summary = "从上级平台移除国标通道")
    @DeleteMapping("/del_channel_for_gb")
    @ResponseBody
    public void delChannelForGB(@RequestBody UpdateChannelParam param) {

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台删除国标通道API调用");
        }
        int result = 0;
        if (param.getChannelReduces() == null || param.getChannelReduces().size() == 0) {
            if (param.isAll()) {
                logger.info("[国标级联]移除所有通道，上级平台， {}", param.getPlatformId());
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
     * 获取目录
     *
     * @param platformId 平台ID
     * @param parentId   目录父ID
     * @return
     */
    @Operation(summary = "获取目录")
    @Parameter(name = "platformId", description = "上级平台的国标编号", required = true)
    @Parameter(name = "parentId", description = "父级目录的国标编号", required = true)
    @GetMapping("/catalog")
    @ResponseBody
    public List<PlatformCatalog> getCatalogByPlatform(String platformId, String parentId) {

        if (logger.isDebugEnabled()) {
            logger.debug("查询目录,platformId: {}, parentId: {}", platformId, parentId);
        }
        ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
        if (platform == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台未找到");
        }
//        if (platformId.equals(parentId)) {
//            parentId = platform.getDeviceGBId();
//        }

        if (platformId.equals(platform.getDeviceGBId())) {
            parentId = null;
        }

        return storager.getChildrenCatalogByPlatform(platformId, parentId);
    }

    /**
     * 添加目录
     *
     * @param platformCatalog 目录
     * @return
     */
    @Operation(summary = "添加目录")
    @PostMapping("/catalog/add")
    @ResponseBody
    public void addCatalog(@RequestBody PlatformCatalog platformCatalog) {

        if (logger.isDebugEnabled()) {
            logger.debug("添加目录,{}", JSON.toJSONString(platformCatalog));
        }
        PlatformCatalog platformCatalogInStore = storager.getCatalog(platformCatalog.getPlatformId(), platformCatalog.getId());

        if (platformCatalogInStore != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), platformCatalog.getId() + " already exists");
        }
        int addResult = storager.addCatalog(platformCatalog);
        if (addResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * 编辑目录
     *
     * @param platformCatalog 目录
     * @return
     */
    @Operation(summary = "编辑目录")
    @PostMapping("/catalog/edit")
    @ResponseBody
    public void editCatalog(@RequestBody PlatformCatalog platformCatalog) {

        if (logger.isDebugEnabled()) {
            logger.debug("编辑目录,{}", JSON.toJSONString(platformCatalog));
        }
        PlatformCatalog platformCatalogInStore = storager.getCatalog(platformCatalog.getPlatformId(), platformCatalog.getId());

        if (platformCatalogInStore == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), platformCatalog.getId() + " not exists");
        }
        int addResult = storager.updateCatalog(platformCatalog);
        if (addResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入数据库失败");
        }
    }

    /**
     * 删除目录
     *
     * @param id 目录Id
     * @param platformId 平台Id
     * @return
     */
    @Operation(summary = "删除目录")
    @Parameter(name = "id", description = "目录Id", required = true)
    @Parameter(name = "platformId", description = "平台Id", required = true)
    @DeleteMapping("/catalog/del")
    @ResponseBody
    public void delCatalog(String id, String platformId) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除目录,{}", id);
        }

        if (ObjectUtils.isEmpty(id) || ObjectUtils.isEmpty(platformId)) {
            throw new ControllerException(ErrorCode.ERROR400);
        }

        int delResult = storager.delCatalog(platformId, id);
        // 如果删除的是默认目录则根目录设置为默认目录
        PlatformCatalog parentPlatform = storager.queryDefaultCatalogInPlatform(platformId);

        // 默认节点被移除
        if (parentPlatform == null) {
            storager.setDefaultCatalog(platformId, platformId);
        }

        if (delResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入数据库失败");
        }
    }

    /**
     * 删除关联
     *
     * @param platformCatalog 关联的信息
     * @return
     */
    @Operation(summary = "删除关联")
    @DeleteMapping("/catalog/relation/del")
    @ResponseBody
    public void delRelation(@RequestBody PlatformCatalog platformCatalog) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除关联,{}", JSON.toJSONString(platformCatalog));
        }
        int delResult = storager.delRelation(platformCatalog);

        if (delResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入数据库失败");
        }
    }


    /**
     * 修改默认目录
     *
     * @param platformId 平台Id
     * @param catalogId  目录Id
     * @return
     */
    @Operation(summary = "修改默认目录")
    @Parameter(name = "catalogId", description = "目录Id", required = true)
    @Parameter(name = "platformId", description = "平台Id", required = true)
    @PostMapping("/catalog/default/update")
    @ResponseBody
    public void setDefaultCatalog(String platformId, String catalogId) {

        if (logger.isDebugEnabled()) {
            logger.debug("修改默认目录,{},{}", platformId, catalogId);
        }
        int updateResult = storager.setDefaultCatalog(platformId, catalogId);

        if (updateResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入数据库失败");
        }
    }


}
