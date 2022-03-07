package com.genersoft.iot.vmp.vmanager.gb28181.platform;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.UpdateChannelParam;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.genersoft.iot.vmp.conf.SipConfig;

import java.util.List;

/**
 * 级联平台管理
 */
@Api(tags = "级联平台管理")
@CrossOrigin
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final static Logger logger = LoggerFactory.getLogger(PlatformController.class);

    @Autowired
    private UserSetup userSetup;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private SipConfig sipConfig;

	@Autowired
	private DynamicTask dynamicTask;

    /**
     * 获取国标服务的配置
     *
     * @return
     */
    @ApiOperation("获取国标服务的配置")
    @GetMapping("/server_config")
    public ResponseEntity<JSONObject> serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getIp());
        result.put("devicePort", sipConfig.getPort());
        result.put("username", sipConfig.getId());
        result.put("password", sipConfig.getPassword());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 获取级联服务器信息
     *
     * @return
     */
    @ApiOperation("获取国标服务的配置")
    @GetMapping("/info/{id}")
    public ResponseEntity<WVPResult<ParentPlatform>> getPlatform(@PathVariable String id) {
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(id);
        WVPResult<ParentPlatform> wvpResult = new WVPResult<>();
        if (parentPlatform != null) {
            wvpResult.setCode(0);
            wvpResult.setMsg("success");
            wvpResult.setData(parentPlatform);
        } else {
            wvpResult.setCode(-1);
            wvpResult.setMsg("未查询到此平台");
        }
        return new ResponseEntity<>(wvpResult, HttpStatus.OK);
    }

    /**
     * 分页查询级联平台
     *
     * @param page  当前页
     * @param count 每页条数
     * @return
     */
    @ApiOperation("分页查询级联平台")
    @GetMapping("/query/{count}/{page}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "当前页", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "count", value = "每页条数", dataTypeClass = Integer.class),
    })
    public PageInfo<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count) {

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询所有上级设备API调用");
//        }
        return storager.queryParentPlatformList(page, count);
    }

    /**
     * 添加上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @ApiOperation("添加上级平台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "parentPlatform", value = "上级平台信息", dataTypeClass = ParentPlatform.class),
    })
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<WVPResult<String>> addPlatform(@RequestBody ParentPlatform parentPlatform) {

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        WVPResult<String> wvpResult = new WVPResult<>();
        if (StringUtils.isEmpty(parentPlatform.getName())
                || StringUtils.isEmpty(parentPlatform.getServerGBId())
                || StringUtils.isEmpty(parentPlatform.getServerGBDomain())
                || StringUtils.isEmpty(parentPlatform.getServerIP())
                || StringUtils.isEmpty(parentPlatform.getServerPort())
                || StringUtils.isEmpty(parentPlatform.getDeviceGBId())
                || StringUtils.isEmpty(parentPlatform.getExpires())
                || StringUtils.isEmpty(parentPlatform.getKeepTimeout())
                || StringUtils.isEmpty(parentPlatform.getTransport())
                || StringUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            wvpResult.setCode(-1);
            wvpResult.setMsg("missing parameters");
            return new ResponseEntity<>(wvpResult, HttpStatus.BAD_REQUEST);
        }
        if (parentPlatform.getServerPort() < 0 || parentPlatform.getServerPort() > 65535) {
            wvpResult.setCode(-1);
            wvpResult.setMsg("error severPort");
            return new ResponseEntity<>(wvpResult, HttpStatus.BAD_REQUEST);
        }

        ParentPlatform parentPlatformOld = storager.queryParentPlatByServerGBId(parentPlatform.getServerGBId());
        if (parentPlatformOld != null) {
            wvpResult.setCode(-1);
            wvpResult.setMsg("平台 " + parentPlatform.getServerGBId() + " 已存在");
            return new ResponseEntity<>(wvpResult, HttpStatus.OK);
        }
        boolean updateResult = storager.updateParentPlatform(parentPlatform);

        if (updateResult) {
            // 保存时启用就发送注册
            if (parentPlatform.isEnable()) {
                if (parentPlatformOld != null && parentPlatformOld.isStatus()) {
                    commanderForPlatform.unregister(parentPlatformOld, null, eventResult -> {
                        //  只要保存就发送注册
                        commanderForPlatform.register(parentPlatform, null, null);
                    });
                } else {
                    //  只要保存就发送注册
                    commanderForPlatform.register(parentPlatform, null, null);
                }

            } else if (parentPlatformOld != null && parentPlatformOld.isEnable() && !parentPlatform.isEnable()) { // 关闭启用时注销
                commanderForPlatform.unregister(parentPlatform, null, null);
            }
            wvpResult.setCode(0);
            wvpResult.setMsg("success");
            return new ResponseEntity<>(wvpResult, HttpStatus.OK);
        } else {
            wvpResult.setCode(-1);
            wvpResult.setMsg("写入数据库失败");
            return new ResponseEntity<>(wvpResult, HttpStatus.OK);
        }
    }

    /**
     * 保存上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @ApiOperation("保存上级平台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "parentPlatform", value = "上级平台信息", dataTypeClass = ParentPlatform.class),
    })
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<WVPResult<String>> savePlatform(@RequestBody ParentPlatform parentPlatform) {

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        WVPResult<String> wvpResult = new WVPResult<>();
        if (StringUtils.isEmpty(parentPlatform.getName())
                || StringUtils.isEmpty(parentPlatform.getServerGBId())
                || StringUtils.isEmpty(parentPlatform.getServerGBDomain())
                || StringUtils.isEmpty(parentPlatform.getServerIP())
                || StringUtils.isEmpty(parentPlatform.getServerPort())
                || StringUtils.isEmpty(parentPlatform.getDeviceGBId())
                || StringUtils.isEmpty(parentPlatform.getExpires())
                || StringUtils.isEmpty(parentPlatform.getKeepTimeout())
                || StringUtils.isEmpty(parentPlatform.getTransport())
                || StringUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            wvpResult.setCode(-1);
            wvpResult.setMsg("missing parameters");
            return new ResponseEntity<>(wvpResult, HttpStatus.BAD_REQUEST);
        }
        ParentPlatform parentPlatformOld = storager.queryParentPlatByServerGBId(parentPlatform.getServerGBId());

        boolean updateResult = storager.updateParentPlatform(parentPlatform);

        if (updateResult) {
            // 保存时启用就发送注册
            if (parentPlatform.isEnable()) {
                if (parentPlatformOld != null && parentPlatformOld.isStatus()) {
                    commanderForPlatform.unregister(parentPlatformOld, null, null);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //  只要保存就发送注册
                    commanderForPlatform.register(parentPlatform, null, null);
                } else {
                    //  只要保存就发送注册
                    commanderForPlatform.register(parentPlatform, null, null);
                }
            } else if (parentPlatformOld != null && parentPlatformOld.isEnable() && !parentPlatform.isEnable()) { // 关闭启用时注销
                commanderForPlatform.unregister(parentPlatformOld, null, null);
            }
            wvpResult.setCode(0);
            wvpResult.setMsg("success");
            return new ResponseEntity<>(wvpResult, HttpStatus.OK);
        } else {
            wvpResult.setCode(0);
            wvpResult.setMsg("写入数据库失败");
            return new ResponseEntity<>(wvpResult, HttpStatus.OK);
        }
    }

    /**
     * 删除上级平台
     *
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @ApiOperation("删除上级平台")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serverGBId", value = "上级平台国标ID", dataTypeClass = String.class),
    })
    @DeleteMapping("/delete/{serverGBId}")
    @ResponseBody
    public ResponseEntity<String> deletePlatform(@PathVariable String serverGBId) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除上级平台API调用");
        }
        if (StringUtils.isEmpty(serverGBId)
        ) {
            return new ResponseEntity<>("missing parameters", HttpStatus.BAD_REQUEST);
        }
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        if (parentPlatform == null) return new ResponseEntity<>("fail", HttpStatus.OK);
        // 发送离线消息,无论是否成功都删除缓存
        commanderForPlatform.unregister(parentPlatform, (event -> {
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

        boolean deleteResult = storager.deleteParentPlatform(parentPlatform);
        storager.delCatalogByPlatformId(parentPlatform.getServerGBId());
        storager.delRelationByPlatformId(parentPlatform.getServerGBId());
        // 停止发送位置订阅定时任务
        String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() +  "_MobilePosition_" + parentPlatform.getServerGBId();
        dynamicTask.stop(key);
        if (deleteResult) {
            return new ResponseEntity<>("success", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }

    /**
     * 查询上级平台是否存在
     *
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @ApiOperation("查询上级平台是否存在")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serverGBId", value = "上级平台国标ID", dataTypeClass = String.class),
    })
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public ResponseEntity<String> exitPlatform(@PathVariable String serverGBId) {

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询上级平台是否存在API调用：" + serverGBId);
//        }
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        return new ResponseEntity<>(String.valueOf(parentPlatform != null), HttpStatus.OK);
    }

    /**
     * 分页查询级联平台的所有所有通道
     *
     * @param page        当前页
     * @param count       每页条数
     * @param platformId  上级平台ID
     * @param query       查询内容
     * @param online      是否在线
     * @param choosed     是否已选中
     * @param channelType 通道类型
     * @return
     */
    @ApiOperation("分页查询级联平台的所有所有通道")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "当前页", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "count", value = "每页条数", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "platformId", value = "上级平台ID", dataTypeClass = String.class),
            @ApiImplicitParam(name = "catalogId", value = "目录ID", dataTypeClass = String.class),
            @ApiImplicitParam(name = "query", value = "查询内容", dataTypeClass = String.class),
            @ApiImplicitParam(name = "online", value = "是否在线", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "channelType", value = "通道类型", dataTypeClass = Boolean.class),
    })
    @GetMapping("/channel_list")
    @ResponseBody
    public PageInfo<ChannelReduce> channelList(int page, int count,
                                               @RequestParam(required = false) String platformId,
                                               @RequestParam(required = false) String catalogId,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Boolean channelType) {

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询所有所有通道API调用");
//        }
        if (StringUtils.isEmpty(platformId)) {
            platformId = null;
        }
        if (StringUtils.isEmpty(query)) {
            query = null;
        }
        if (StringUtils.isEmpty(platformId) || StringUtils.isEmpty(catalogId)) {
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
    @ApiOperation("向上级平台添加国标通道")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "通道关联参数", dataTypeClass = UpdateChannelParam.class),
    })
    @PostMapping("/update_channel_for_gb")
    @ResponseBody
    public ResponseEntity<String> updateChannelForGB(@RequestBody UpdateChannelParam param) {

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台添加国标通道API调用");
        }
        int result = storager.updateChannelForGB(param.getPlatformId(), param.getChannelReduces(), param.getCatalogId());

        return new ResponseEntity<>(String.valueOf(result > 0), HttpStatus.OK);
    }

    /**
     * 从上级平台移除国标通道
     *
     * @param param 通道关联参数
     * @return
     */
    @ApiOperation("从上级平台移除国标通道")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "通道关联参数", dataTypeClass = UpdateChannelParam.class),
    })
    @DeleteMapping("/del_channel_for_gb")
    @ResponseBody
    public ResponseEntity<String> delChannelForGB(@RequestBody UpdateChannelParam param) {

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台删除国标通道API调用");
        }
        int result = storager.delChannelForGB(param.getPlatformId(), param.getChannelReduces());

        return new ResponseEntity<>(String.valueOf(result > 0), HttpStatus.OK);
    }

    /**
     * 获取目录
     *
     * @param platformId 平台ID
     * @param parentId   目录父ID
     * @return
     */
    @ApiOperation("获取目录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "platformId", value = "平台ID", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "parentId", value = "目录父ID", dataTypeClass = String.class, required = true),
    })
    @GetMapping("/catalog")
    @ResponseBody
    public ResponseEntity<WVPResult<List<PlatformCatalog>>> getCatalogByPlatform(String platformId, String parentId) {

        if (logger.isDebugEnabled()) {
            logger.debug("查询目录,platformId: {}, parentId: {}", platformId, parentId);
        }
        List<PlatformCatalog> platformCatalogList = storager.getChildrenCatalogByPlatform(platformId, parentId);
        // 查询下属的国标通道
//        List<PlatformCatalog> catalogsForChannel = storager.queryChannelInParentPlatformAndCatalog(platformId, parentId);
        // 查询下属的直播流通道
//        List<PlatformCatalog> catalogsForStream = storager.queryStreamInParentPlatformAndCatalog(platformId, parentId);
//        platformCatalogList.addAll(catalogsForChannel);
//        platformCatalogList.addAll(catalogsForStream);
        WVPResult<List<PlatformCatalog>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(platformCatalogList);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 添加目录
     *
     * @param platformCatalog 目录
     * @return
     */
    @ApiOperation("添加目录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "platformCatalog", value = "目录信息", dataTypeClass = PlatformCatalog.class, required = true),
    })
    @PostMapping("/catalog/add")
    @ResponseBody
    public ResponseEntity<WVPResult<List<PlatformCatalog>>> addCatalog(@RequestBody PlatformCatalog platformCatalog) {

        if (logger.isDebugEnabled()) {
            logger.debug("添加目录,{}", JSON.toJSONString(platformCatalog));
        }
        PlatformCatalog platformCatalogInStore = storager.getCatalog(platformCatalog.getId());
        WVPResult<List<PlatformCatalog>> result = new WVPResult<>();


        if (platformCatalogInStore != null) {
            result.setCode(-1);
            result.setMsg(platformCatalog.getId() + " already exists");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        int addResult = storager.addCatalog(platformCatalog);
        if (addResult > 0) {
            result.setCode(0);
            result.setMsg("success");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.setCode(-500);
            result.setMsg("save error");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    /**
     * 编辑目录
     *
     * @param platformCatalog 目录
     * @return
     */
    @ApiOperation("编辑目录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "platformCatalog", value = "目录信息", dataTypeClass = PlatformCatalog.class, required = true),
    })
    @PostMapping("/catalog/edit")
    @ResponseBody
    public ResponseEntity<WVPResult<List<PlatformCatalog>>> editCatalog(@RequestBody PlatformCatalog platformCatalog) {

        if (logger.isDebugEnabled()) {
            logger.debug("编辑目录,{}", JSON.toJSONString(platformCatalog));
        }
        PlatformCatalog platformCatalogInStore = storager.getCatalog(platformCatalog.getId());
        WVPResult<List<PlatformCatalog>> result = new WVPResult<>();
        result.setCode(0);

        if (platformCatalogInStore == null) {
            result.setMsg(platformCatalog.getId() + " not exists");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        int addResult = storager.updateCatalog(platformCatalog);
        if (addResult > 0) {
            result.setMsg("success");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.setMsg("save error");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    /**
     * 删除目录
     *
     * @param id 目录Id
     * @return
     */
    @ApiOperation("删除目录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "目录Id", dataTypeClass = String.class, required = true),
    })
    @DeleteMapping("/catalog/del")
    @ResponseBody
    public ResponseEntity<WVPResult<String>> delCatalog(String id, String platformId) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除目录,{}", id);
        }
        WVPResult<String> result = new WVPResult<>();

        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(platformId)) {
            result.setCode(-1);
            result.setMsg("param error");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
        result.setCode(0);

        int delResult = storager.delCatalog(id);
        // 如果删除的是默认目录则根目录设置为默认目录
        PlatformCatalog parentPlatform = storager.queryDefaultCatalogInPlatform(platformId);

        // 默认节点被移除
        if (parentPlatform == null) {
            storager.setDefaultCatalog(platformId, platformId);
            result.setData(platformId);
        }


        if (delResult > 0) {
            result.setMsg("success");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.setMsg("save error");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    /**
     * 删除关联
     *
     * @param platformCatalog 关联的信息
     * @return
     */
    @ApiOperation("删除关联")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "platformCatalog", value = "关联的信息", dataTypeClass = PlatformCatalog.class, required = true),
    })
    @DeleteMapping("/catalog/relation/del")
    @ResponseBody
    public ResponseEntity<WVPResult<List<PlatformCatalog>>> delRelation(@RequestBody PlatformCatalog platformCatalog) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除关联,{}", JSON.toJSONString(platformCatalog));
        }
        int delResult = storager.delRelation(platformCatalog);
        WVPResult<List<PlatformCatalog>> result = new WVPResult<>();
        result.setCode(0);

        if (delResult > 0) {
            result.setMsg("success");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.setMsg("save error");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }


    /**
     * 修改默认目录
     *
     * @param platformId 平台Id
     * @param catalogId  目录Id
     * @return
     */
    @ApiOperation("修改默认目录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "platformId", value = "平台Id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "catalogId", value = "目录Id", dataTypeClass = String.class, required = true),
    })
    @PostMapping("/catalog/default/update")
    @ResponseBody
    public ResponseEntity<WVPResult<String>> setDefaultCatalog(String platformId, String catalogId) {

        if (logger.isDebugEnabled()) {
            logger.debug("修改默认目录,{},{}", platformId, catalogId);
        }
        int updateResult = storager.setDefaultCatalog(platformId, catalogId);
        WVPResult<String> result = new WVPResult<>();
        result.setCode(0);

        if (updateResult > 0) {
            result.setMsg("success");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.setMsg("save error");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }


}
