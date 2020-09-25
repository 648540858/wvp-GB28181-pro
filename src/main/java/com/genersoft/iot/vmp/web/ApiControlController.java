package com.genersoft.iot.vmp.web;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.ptz.PtzController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 兼容LiveGBS的API：设备控制
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/api/v1/control")
public class ApiControlController {

    private final static Logger logger = LoggerFactory.getLogger(ApiControlController.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorager storager;

    /**
     * 设备控制 - 云台控制
     * @param serial 设备编号
     * @param command 控制指令 允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop
     * @param channel 通道序号
     * @param code 通道编号
     * @param speed 速度(0~255) 默认值: 129
     * @return
     */
    @RequestMapping(value = "/ptz")
    private JSONObject list(String serial,String command,
                            @RequestParam(required = false)Integer channel,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)Integer speed){

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("模拟接口> 设备云台控制 API调用，deviceId：%s ，channelId：%s ，command：%d ，speed：%d ",
                    serial, code, command, speed));
        }
        Device device = storager.queryVideoDevice(serial);
        int leftRight = 0;
        int upDown = 0;
        int inOut = 0;
        switch (command) {
            case "left":
                leftRight = 1;
                break;
            case "right":
                leftRight = 2;
                break;
            case "up":
                upDown = 1;
                break;
            case "down":
                upDown = 2;
                break;
            case "upleft":
                upDown = 1;
                leftRight = 1;
            case "upright":
                upDown = 1;
                leftRight = 2;
                break;
            case "downleft":
                upDown = 2;
                leftRight = 1;
                break;
            case "downright":
                upDown = 2;
                leftRight = 2;
                break;
            case "zoomin":
                inOut = 2;
                break;
            case "zoomout":
                inOut = 1;
                break;
            case "stop":
                break;

        }
        // 默认值 50
        cmder.ptzCmd(device, code, leftRight, upDown, inOut, speed==0 ? 129 : speed, 50);
        return null;
    }
}
