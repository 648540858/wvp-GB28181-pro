package com.genersoft.iot.vmp.vmanager.platform;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.vmanager.platform.bean.UpdateChannelParam;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
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
@Api("级联平台管理")
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

    @GetMapping("/server_config")
    public ResponseEntity<JSONObject> serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getSipIp());
        result.put("devicePort", sipConfig.getSipPort());
        result.put("username", sipConfig.getSipId());
        result.put("password", sipConfig.getSipPassword());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/query/{count}/{page}")
    public PageInfo<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有上级设备API调用");
        }
        return storager.queryParentPlatformList(page, count);
    }

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
        ParentPlatform parentPlatformOld = storager.queryParentPlatById(parentPlatform.getServerGBId());

        boolean updateResult = storager.updateParentPlatform(parentPlatform);

        if (updateResult) {
            // 保存时启用就发送注册
            if (parentPlatform.isEnable()) {
                //  只要保存就发送注册
                commanderForPlatform.register(parentPlatform);
            } else if (parentPlatformOld != null && parentPlatformOld.isEnable() && !parentPlatform.isEnable()){ // 关闭启用时注销
                commanderForPlatform.unregister(parentPlatform, null, null);
            }

 
            return new ResponseEntity<>("success", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }

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
        ParentPlatform parentPlatform = storager.queryParentPlatById(serverGBId);
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

    @GetMapping("/exit/{deviceGbId}")
    @ResponseBody
    public ResponseEntity<String> exitPlatform(@PathVariable String deviceGbId){

        if (logger.isDebugEnabled()) {
            logger.debug("查询上级平台是否存在API调用：" + deviceGbId);
        }
        ParentPlatform parentPlatform = storager.queryParentPlatById(deviceGbId);
        return new ResponseEntity<>(String.valueOf(parentPlatform != null), HttpStatus.OK);
    }

    @GetMapping("/channel_list")
    @ResponseBody
    public PageInfo<ChannelReduce> channelList(int page, int count,
                                              @RequestParam(required = false) String platformId,
                                              @RequestParam(required = false) String query,
                                              @RequestParam(required = false) Boolean online,
                                              @RequestParam(required = false) Boolean choosed,
                                              @RequestParam(required = false) Boolean channelType){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有所有通道API调用");
        }
        PageInfo<ChannelReduce> channelReduces = null;
        if (platformId != null ) {
            channelReduces = storager.queryAllChannelList(page, count, query, online, channelType, platformId, choosed);
        }else {
            channelReduces = storager.queryAllChannelList(page, count, query, online, channelType, null, false);
        }

        return channelReduces;
    }


    @PostMapping("/update_channel_for_gb")
    @ResponseBody
    public ResponseEntity<String> updateChannelForGB(@RequestBody UpdateChannelParam param){

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台添加国标通道API调用");
        }
        int result = storager.updateChannelForGB(param.getPlatformId(), param.getChannelReduces());

        return new ResponseEntity<>(String.valueOf(result > 0), HttpStatus.OK);
    }

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
