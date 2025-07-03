package com.genersoft.iot.vmp.web.gb28181;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * API兼容：设备控制
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/control")
public class ApiControlController {

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IDeviceService deviceService;

    /**
     * 设备控制 - 云台控制
     * @param serial 设备编号
     * @param command 控制指令 允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop
     * @param channel 通道序号
     * @param code 通道编号
     * @param speed 速度(0~255) 默认值: 129
     */
    @GetMapping(value = "/ptz")
    private void ptz(String serial,String command,
                            @RequestParam(required = false)Integer channel,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)Integer speed){

        if (log.isDebugEnabled()) {
            log.debug("模拟接口> 设备云台控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",
                    serial, code, command, speed);
        }
        if (channel == null) {channel = 0;}
        if (speed == null) {speed = 0;}
        Device device = deviceService.getDeviceByDeviceId(serial);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "device[ " + serial + " ]未找到");
        }
        int cmdCode = -1;
        switch (command){
            case "left":
                cmdCode = 2;
                break;
            case "right":
                cmdCode = 1;
                break;
            case "up":
                cmdCode = 8;
                break;
            case "down":
                cmdCode = 4;
                break;
            case "upleft":
                cmdCode = 10;
                break;
            case "upright":
                cmdCode = 9;
                break;
            case "downleft":
                cmdCode = 6;
                break;
            case "downright":
                cmdCode = 5;
                break;
            case "zoomin":
                cmdCode = 16;
                break;
            case "zoomout":
                cmdCode = 32;
                break;
            case "stop":
                cmdCode = 0;
                break;
            default:
                break;
        }
        if (cmdCode == -1) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未识别的指令：" + command);
        }
        // 默认值 50
        try {
            cmder.frontEndCmd(device, code, cmdCode, speed, speed, speed);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 云台控制: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    /**
     * 设备控制 - 预置位控制
     * @param serial 设备编号
     * @param code 通道编号,通过 /api/v1/device/channellist 获取的 ChannelList.ID, 该参数和 channel 二选一传递即可
     * @param channel 通道序号, 默认值: 1
     * @param command 控制指令 允许值: set, goto, remove
     * @param preset 预置位编号(1~255)
     * @param name 预置位名称, command=set 时有效
     */
    @GetMapping(value = "/preset")
    private void list(String serial,String command,
                            @RequestParam(required = false)Integer channel,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)String name,
                            @RequestParam(required = false)Integer preset){

        if (log.isDebugEnabled()) {
            log.debug("模拟接口> 预置位控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，name：{} ，preset：{} ",
                    serial, code, command, name, preset);
        }

        if (channel == null) {channel = 0;}
        Device device = deviceService.getDeviceByDeviceId(serial);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "device[ " + serial + " ]未找到");
        }
        int cmdCode = 0;
        switch (command){
            case "set":
                cmdCode = 129;
                break;
            case "goto":
                cmdCode = 130;
                break;
            case "remove":
                cmdCode = 131;
                break;
            default:
                break;
        }
        try {
            cmder.frontEndCmd(device, code, cmdCode, 0, preset, 0);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 预置位控制: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }
}
