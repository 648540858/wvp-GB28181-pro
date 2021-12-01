package com.genersoft.iot.vmp.vmanager.gb28181.platform;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
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
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

	@Autowired
	private SipConfig sipConfig;

    /**
     * 获取国标服务的配置
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
     * 分页查询级联平台
     * @param page 当前页
     * @param count 每页条数
     * @return
     */
    @ApiOperation("分页查询级联平台")
    @GetMapping("/query/{count}/{page}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "当前页", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "count", value = "每页条数", dataTypeClass = Integer.class),
    })
    public PageInfo<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count){

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询所有上级设备API调用");
//        }
        return storager.queryParentPlatformList(page, count);
    }

    /**
     * 保存上级平台信息
     * @param parentPlatform
     * @return
     */
    @ApiOperation("保存上级平台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "parentPlatform", value = "上级平台信息", dataTypeClass = ParentPlatform.class),
    })
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<String> savePlatform(@RequestBody ParentPlatform parentPlatform){

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        if (StringUtils.isEmpty(parentPlatform.getName())
                ||StringUtils.isEmpty(parentPlatform.getServerGBId())
                ||StringUtils.isEmpty(parentPlatform.getServerGBDomain())
                ||StringUtils.isEmpty(parentPlatform.getServerIP())
                ||StringUtils.isEmpty(parentPlatform.getServerPort())
                ||StringUtils.isEmpty(parentPlatform.getDeviceGBId())
                ||StringUtils.isEmpty(parentPlatform.getExpires())
                ||StringUtils.isEmpty(parentPlatform.getKeepTimeout())
                ||StringUtils.isEmpty(parentPlatform.getTransport())
                ||StringUtils.isEmpty(parentPlatform.getCharacterSet())
        ){
            return new ResponseEntity<>("missing parameters", HttpStatus.BAD_REQUEST);
        }
        // TODO 检查是否已经存在,且注册成功, 如果注册成功,需要先注销之前再,修改并注册

        // ParentPlatform parentPlatformOld = storager.queryParentPlatById(parentPlatform.getDeviceGBId());
        ParentPlatform parentPlatformOld = storager.queryParentPlatByServerGBId(parentPlatform.getServerGBId());

        boolean updateResult = storager.updateParentPlatform(parentPlatform);

        if (updateResult) {
            // 保存时启用就发送注册
            if (parentPlatform.isEnable()) {
                //  只要保存就发送注册
                commanderForPlatform.register(parentPlatform, null, null);
            } else if (parentPlatformOld != null && parentPlatformOld.isEnable() && !parentPlatform.isEnable()){ // 关闭启用时注销
                commanderForPlatform.unregister(parentPlatform, null, null);
            }
            return new ResponseEntity<>("success", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }

    /**
     * 删除上级平台
      * @param serverGBId 上级平台国标ID
     * @return
     */
    @ApiOperation("删除上级平台")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serverGBId", value = "上级平台国标ID", dataTypeClass = String.class),
    })
    @DeleteMapping("/delete/{serverGBId}")
    @ResponseBody
    public ResponseEntity<String> deletePlatform(@PathVariable String serverGBId){

        if (logger.isDebugEnabled()) {
            logger.debug("删除上级平台API调用");
        }
        if (StringUtils.isEmpty(serverGBId)
        ){
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


        if (deleteResult) {
            return new ResponseEntity<>("success", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }

    /**
     * 查询上级平台是否存在
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @ApiOperation("查询上级平台是否存在")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serverGBId", value = "上级平台国标ID", dataTypeClass = String.class),
    })
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public ResponseEntity<String> exitPlatform(@PathVariable String serverGBId){

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询上级平台是否存在API调用：" + serverGBId);
//        }
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        return new ResponseEntity<>(String.valueOf(parentPlatform != null), HttpStatus.OK);
    }

    /**
     * 分页查询级联平台的所有所有通道
     * @param page 当前页
     * @param count 每页条数
     * @param platformId 上级平台ID
     * @param query 查询内容
     * @param online 是否在线
     * @param choosed 是否已选中
     * @param channelType 通道类型
     * @return
     */
    @ApiOperation("分页查询级联平台的所有所有通道")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "当前页", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "count", value = "每页条数", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "platformId", value = "上级平台ID", dataTypeClass = String.class),
            @ApiImplicitParam(name = "query", value = "查询内容", dataTypeClass = String.class),
            @ApiImplicitParam(name = "online", value = "是否在线", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "choosed", value = "是否已选中", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "channelType", value = "通道类型", dataTypeClass = Boolean.class),
    })
    @GetMapping("/channel_list")
    @ResponseBody
    public PageInfo<ChannelReduce> channelList(int page, int count,
                                              @RequestParam(required = false) String platformId,
                                              @RequestParam(required = false) String query,
                                              @RequestParam(required = false) Boolean online,
                                              @RequestParam(required = false) Boolean choosed,
                                              @RequestParam(required = false) Boolean channelType){

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询所有所有通道API调用");
//        }
        PageInfo<ChannelReduce> channelReduces = null;
        if (platformId != null ) {
            channelReduces = storager.queryAllChannelList(page, count, query, online, channelType, platformId, choosed);
        }else {
            channelReduces = storager.queryAllChannelList(page, count, query, online, channelType, null, false);
        }

        return channelReduces;
    }

    /**
     * 向上级平台添加国标通道
     * @param param 通道关联参数
     * @return
     */
    @ApiOperation("向上级平台添加国标通道")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "通道关联参数", dataTypeClass = UpdateChannelParam.class),
    })
    @PostMapping("/update_channel_for_gb")
    @ResponseBody
    public ResponseEntity<String> updateChannelForGB(@RequestBody UpdateChannelParam param){

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台添加国标通道API调用");
        }
        int result = storager.updateChannelForGB(param.getPlatformId(), param.getChannelReduces());

        return new ResponseEntity<>(String.valueOf(result > 0), HttpStatus.OK);
    }

    /**
     * 从上级平台移除国标通道
     * @param param 通道关联参数
     * @return
     */
    @ApiOperation("从上级平台移除国标通道")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "通道关联参数", dataTypeClass = UpdateChannelParam.class),
    })
    @DeleteMapping("/del_channel_for_gb")
    @ResponseBody
    public ResponseEntity<String> delChannelForGB(@RequestBody UpdateChannelParam param){

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台删除国标通道API调用");
        }
        int result = storager.delChannelForGB(param.getPlatformId(), param.getChannelReduces());

        return new ResponseEntity<>(String.valueOf(result > 0), HttpStatus.OK);
    }


}
