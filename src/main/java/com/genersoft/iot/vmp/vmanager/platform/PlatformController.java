package com.genersoft.iot.vmp.vmanager.platform;

import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.device.DeviceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PlatformController {

    private final static Logger logger = LoggerFactory.getLogger(PlatformController.class);

    @Autowired
    private IVideoManagerStorager storager;

    @GetMapping("/platforms/{count}/{page}")
    public PageResult<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count){

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
        boolean updateResult = storager.updateParentPlatform(parentPlatform);
        if (updateResult) {
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
        boolean deleteResult = storager.deleteParentPlatform(parentPlatform);
        if (deleteResult) {
            return new ResponseEntity<>("success", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("fail", HttpStatus.OK);
        }
    }
}
