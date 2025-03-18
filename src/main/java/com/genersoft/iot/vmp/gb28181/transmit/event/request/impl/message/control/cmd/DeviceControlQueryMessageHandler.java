package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.cmd;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.ControlMessageHandler;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;
import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.loadElement;

@Slf4j
@Component
public class DeviceControlQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "DeviceControl";

    @Autowired
    private ControlMessageHandler controlMessageHandler;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelControlService channelControlService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private SIPCommander cmder;

    @Override
    public void afterPropertiesSet() throws Exception {
        controlMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();

        // 此处是上级发出的DeviceControl指令
        String targetGBId = ((SipURI) request.getToHeader().getAddress().getURI()).getUser();
        String channelId = getText(rootElement, "DeviceID");
        // 远程启动功能
        if (!ObjectUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
            // 拒绝远程启动命令
            log.warn("[deviceControl] 远程启动命令， 禁用，不允许上级平台随意重启下级平台");
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        DeviceControlType deviceControlType = DeviceControlType.typeOf(rootElement);

        CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), channelId);
        if (channel == null) {
            log.warn("[deviceControl] 未找到通道， 平台： {}（{}），通道编号：{}", platform.getName(),
                    platform.getServerGBId(), channelId);
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] 命令: {}, 平台： {}（{}）->{}", deviceControlType, platform.getName(),
                platform.getServerGBId(), channel.getGbId());

        if (!ObjectUtils.isEmpty(deviceControlType)) {
            switch (deviceControlType) {
                case PTZ:
                    handlePtzCmd(channel, rootElement, request, DeviceControlType.PTZ);
                    break;
                case ALARM:
                    handleAlarmCmd(channel, rootElement, request);
                    break;
                case GUARD:
                    handleGuardCmd(channel, rootElement, request, DeviceControlType.GUARD);
                    break;
                case RECORD:
                    handleRecordCmd(channel, rootElement, request, DeviceControlType.RECORD);
                    break;
                case I_FRAME:
                    handleIFameCmd(channel, request);
                    break;
                case TELE_BOOT:
                    handleTeleBootCmd(channel, request);
                    break;
                case DRAG_ZOOM_IN:
                    handleDragZoom(channel, rootElement, request, DeviceControlType.DRAG_ZOOM_IN);
                    break;
                case DRAG_ZOOM_OUT:
                    handleDragZoom(channel, rootElement, request, DeviceControlType.DRAG_ZOOM_OUT);
                    break;
                case HOME_POSITION:
                    handleHomePositionCmd(channel, rootElement, request, DeviceControlType.HOME_POSITION);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 处理云台指令
     */
    private void handlePtzCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() == ChannelDataType.GB28181.value) {

            deviceChannelService.handlePtzCmd(channel.getDataDeviceId(), channel.getGbId(), rootElement, type, ((code, msg, data) -> {
                try {
                    responseAck(request, code, msg);
                }  catch (InvalidArgumentException | SipException | ParseException exception) {
                    log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                }
            }));
        }else {
            // 解析云台控制参数
            String cmdString = getText(rootElement, type.getVal());
            IFrontEndControlCode frontEndControlCode = FrontEndCode.decode(cmdString);
            if (frontEndControlCode == null) {
                log.info("[INFO 消息] 不支持的控制方式");
                try {
                    responseAck(request, Response.FORBIDDEN, "");
                }  catch (InvalidArgumentException | SipException | ParseException exception) {
                    log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                }
                return;
            }
            switch (frontEndControlCode.getType()){
                case PTZ:
                    channelControlService.ptz(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            responseAck(request, code, msg);
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                        }
                    }));
                    break;
                case FI:
                    channelControlService.fi(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            responseAck(request, code, msg);
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                        }
                    }));
                    break;
                case PRESET:
                    channelControlService.preset(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            responseAck(request, code, msg);
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                        }
                    }));
                    break;
                case TOUR:
                    channelControlService.tour(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            responseAck(request, code, msg);
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                        }
                    }));
                    break;
                case SCAN:
                    channelControlService.scan(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            responseAck(request, code, msg);
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                        }
                    }));
                    break;
                case AUXILIARY:
                    channelControlService.auxiliary(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            responseAck(request, code, msg);
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                        }
                    }));
                    break;
                default:
                    log.info("[INFO 消息] 设备不支持的控制方式");
                    try {
                        responseAck(request, Response.FORBIDDEN, "");
                    }  catch (InvalidArgumentException | SipException | ParseException exception) {
                        log.error("[命令发送失败] 云台指令: {}", exception.getMessage());
                    }
            }
        }
    }

    /**
     * 处理强制关键帧
     */
    private void handleIFameCmd(CommonGBChannel channel, SIPRequest request) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[INFO 消息] 只支持国标的处理强制关键帧， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[deviceControl] 未找到设备原始通道， 设备： {}（{}），通道编号：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] 命令: 强制关键帧, 设备： {}（{}）， 通道{}（{}",  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        try {
            cmder.iFrameCmd(device, deviceChannel.getDeviceId());
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 强制关键帧: {}", e.getMessage());
        }
    }

    /**
     * 处理重启命令
     */
    private void handleTeleBootCmd(CommonGBChannel channel, SIPRequest request) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[INFO 消息] 只支持国标的重启命令， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        try {
            cmder.teleBootCmd(device);
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 重启: {}", e.getMessage());
        }

    }

    /**
     * 处理拉框控制
     */
    private void handleDragZoom(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[deviceControl-DragZoom] 只支持国标的拉框控制， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[deviceControl-DragZoom] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[deviceControl-DragZoom] 未找到设备原始通道， 设备： {}（{}），通道编号：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] 命令: {}, 设备： {}（{}）， 通道{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        try {
            DragZoomRequest dragZoomRequest = loadElement(rootElement, DragZoomRequest.class);
            DragZoomParam dragZoom = dragZoomRequest.getDragZoomIn();
            if (dragZoom == null) {
                dragZoom = dragZoomRequest.getDragZoomOut();
            }
            StringBuffer cmdXml = new StringBuffer(200);
            cmdXml.append("<" + type.getVal() + ">\r\n");
            cmdXml.append("<Length>" + dragZoom.getLength() + "</Length>\r\n");
            cmdXml.append("<Width>" + dragZoom.getWidth() + "</Width>\r\n");
            cmdXml.append("<MidPointX>" + dragZoom.getMidPointX() + "</MidPointX>\r\n");
            cmdXml.append("<MidPointY>" + dragZoom.getMidPointY() + "</MidPointY>\r\n");
            cmdXml.append("<LengthX>" + dragZoom.getLengthX() + "</LengthX>\r\n");
            cmdXml.append("<LengthY>" + dragZoom.getLengthY() + "</LengthY>\r\n");
            cmdXml.append("</" + type.getVal() + ">\r\n");
            cmder.dragZoomCmd(device, deviceChannel.getDeviceId(), cmdXml.toString(), (code, msg, data) -> {

            });
            responseAck(request, Response.OK);
        } catch (Exception e) {
            log.error("[命令发送失败] 拉框控制: {}", e.getMessage());
        }

    }

    /**
     * 处理看守位命令
     */
    private void handleHomePositionCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[INFO 消息] 只支持国标的看守位命令， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[deviceControl] 未找到设备原始通道， 设备： {}（{}），通道编号：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] 命令: {}, 设备： {}（{}）， 通道{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        try {
            HomePositionRequest homePosition = loadElement(rootElement, HomePositionRequest.class);
            //获取整个消息主体，我们只需要修改请求头即可
            HomePositionRequest.HomePosition info = homePosition.getHomePosition();
            cmder.homePositionCmd(device, deviceChannel.getDeviceId(), !"0".equals(info.getEnabled()), Integer.parseInt(info.getResetTime()), Integer.parseInt(info.getPresetIndex()), (code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    onOk(request);
                }else {
                    onError(request, code, msg);
                }
            });
        } catch (Exception e) {
            log.error("[命令发送失败] 看守位设置: {}", e.getMessage());
        }
    }

    /**
     * 处理告警消息
     */
    private void handleAlarmCmd(CommonGBChannel channel, Element rootElement, SIPRequest request) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[INFO 消息] 只支持国标的告警消息， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        //告警方法
        String alarmMethod = "";
        //告警类型
        String alarmType = "";
        List<Element> info = rootElement.elements("Info");
        if (info != null) {
            for (Element element : info) {
                alarmMethod = getText(element, "AlarmMethod");
                alarmType = getText(element, "AlarmType");
            }
        }
        try {
            cmder.alarmResetCmd(device, alarmMethod, alarmType, (code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    onOk(request);
                }else {
                    onError(request, code, msg);
                }
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 告警消息: {}", e.getMessage());
        }
    }

    /**
     * 处理录像控制
     */
    private void handleRecordCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[INFO 消息] 只支持国标的息录像控制， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            // 拒绝远程启动命令
            log.warn("[deviceControl] 未找到设备原始通道， 设备： {}（{}），通道编号：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] 命令: {}, 设备： {}（{}）， 通道{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.recordCmd(device, deviceChannel.getDeviceId(), cmdString, (code, msg, data) -> {
                        if (code == ErrorCode.SUCCESS.getCode()) {
                            onOk(request);
                        }else {
                            onError(request, code, msg);
                        }
                    });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像控制: {}", e.getMessage());
        }
    }

    /**
     * 处理报警布防/撤防命令
     */
    private void handleGuardCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            // 只支持国标的云台控制
            log.warn("[INFO 消息] 只支持国标的报警布防/撤防命令， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.guardCmd(device, cmdString,(code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    onOk(request);
                }else {
                    onError(request, code, msg);
                }
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 布防/撤防命令: {}", e.getMessage());
        }
    }




    /**
     * 错误响应处理
     *
     */
    private void onError(SIPRequest request, Integer code, String msg) {
        // 失败的回复
        try {
            responseAck(request, code, msg);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }

    private void onError(SIPRequest request, SipSubscribe.EventResult errorResult) {
        onError(request, errorResult.statusCode, errorResult.msg);
    }

    /**
     * 成功响应处理
     *
     * @param request     请求
     */
    private void onOk(SIPRequest request) {
        // 成功的回复
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }
}
