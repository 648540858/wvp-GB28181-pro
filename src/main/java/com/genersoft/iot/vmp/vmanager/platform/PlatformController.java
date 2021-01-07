package com.genersoft.iot.vmp.vmanager.platform;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.genersoft.iot.vmp.conf.SipConfig;


@CrossOrigin
@RestController
@RequestMapping("/api")
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

    @GetMapping("/platforms/serverconfig")
    public ResponseEntity<JSONObject> serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getSipIp());
        result.put("devicePort", sipConfig.getSipPort());
        result.put("username", sipConfig.getSipId());
        result.put("password", sipConfig.getSipPassword());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/platforms/{count}/{page}")
    public PageInfo<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有上级设备API调用");
        }
        return storager.queryParentPlatformList(page, count);
    }

    @RequestMapping("/platforms/save")
    @ResponseBody
    public ResponseEntity<String> savePlatform(@RequestBody ParentPlatform parentPlatform){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有上级设备API调用");
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

        ParentPlatform parentPlatformOld = storager.queryParentPlatById(parentPlatform.getDeviceGBId());

        boolean updateResult = storager.updateParentPlatform(parentPlatform);

        if (updateResult) {
            // 保存时启用就发送注册
            if (parentPlatform.isEnable()) {
                //  只要保存就发送注册
                commanderForPlatform.register(parentPlatform);
            }else if (parentPlatformOld != null && parentPlatformOld.isEnable() && !parentPlatform.isEnable()){ // 关闭启用时注销
                commanderForPlatform.unregister(parentPlatform, null, null);
            }


            return new ResponseEntity<>("success", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }

    @RequestMapping("/platforms/delete")
    @ResponseBody
    public ResponseEntity<String> deletePlatform(@RequestBody ParentPlatform parentPlatform){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有上级设备API调用");
        }
        if (StringUtils.isEmpty(parentPlatform.getDeviceGBId())
        ){
            return new ResponseEntity<>("missing parameters", HttpStatus.BAD_REQUEST);
        }

        // 发送离线消息,无论是否成功都删除缓存
        commanderForPlatform.unregister(parentPlatform, (event -> {
            // 清空redis缓存
            redisCatchStorage.delPlatformCatchInfo(parentPlatform.getDeviceGBId());
            redisCatchStorage.delPlatformKeepalive(parentPlatform.getDeviceGBId());
            redisCatchStorage.delPlatformRegister(parentPlatform.getDeviceGBId());
        }), (event -> {
            // 清空redis缓存
            redisCatchStorage.delPlatformCatchInfo(parentPlatform.getDeviceGBId());
            redisCatchStorage.delPlatformKeepalive(parentPlatform.getDeviceGBId());
            redisCatchStorage.delPlatformRegister(parentPlatform.getDeviceGBId());
        }));

        boolean deleteResult = storager.deleteParentPlatform(parentPlatform);


        if (deleteResult) {
            return new ResponseEntity<>("success", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }

    @RequestMapping("/platforms/exit/{deviceGbId}")
    @ResponseBody
    public ResponseEntity<String> exitPlatform(@PathVariable String deviceGbId){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有上级设备API调用");
        }
        ParentPlatform parentPlatform = storager.queryParentPlatById(deviceGbId);
        return new ResponseEntity<>(String.valueOf(parentPlatform != null), HttpStatus.OK);
    }


}
