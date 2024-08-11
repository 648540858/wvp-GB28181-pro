package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.cmd;

import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DragZoomRequest;
import com.genersoft.iot.vmp.gb28181.bean.HomePositionRequest;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.ControlMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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
    private IVideoManagerStorage storager;

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
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();

        // 此处是上级发出的DeviceControl指令
        String targetGBId = ((SipURI) request.getToHeader().getAddress().getURI()).getUser();
        String channelId = getText(rootElement, "DeviceID");
        // 远程启动功能
        if (!ObjectUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
            // 拒绝远程启动命令
            log.warn("[国标级联]收到平台的远程启动命令， 禁用，不允许上级平台随意重启下级平台");
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        DeviceControlType deviceControlType = DeviceControlType.typeOf(rootElement);
        log.info("[接受deviceControl命令] 命令: {}", deviceControlType);
        if (!ObjectUtils.isEmpty(deviceControlType) && !parentPlatform.getServerGBId().equals(targetGBId)) {
            //判断是否存在该通道
            Device deviceForPlatform = storager.queryVideoDeviceByPlatformIdAndChannelId(parentPlatform.getServerGBId(), channelId);
            if (deviceForPlatform == null) {
                try {
                    responseAck(request, Response.NOT_FOUND);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 错误信息: {}", e.getMessage());
                }
                return;
            }
            switch (deviceControlType) {
                case PTZ:
                    handlePtzCmd(deviceForPlatform, channelId, rootElement, request, DeviceControlType.PTZ);
                    break;
                case ALARM:
                    handleAlarmCmd(deviceForPlatform, rootElement, request);
                    break;
                case GUARD:
                    handleGuardCmd(deviceForPlatform, rootElement, request, DeviceControlType.GUARD);
                    break;
                case RECORD:
                    handleRecordCmd(deviceForPlatform, channelId, rootElement, request, DeviceControlType.RECORD);
                    break;
                case I_FRAME:
                    handleIFameCmd(deviceForPlatform, request, channelId);
                    break;
                case TELE_BOOT:
                    handleTeleBootCmd(deviceForPlatform, request);
                    break;
                case DRAG_ZOOM_IN:
                    handleDragZoom(deviceForPlatform, channelId, rootElement, request, DeviceControlType.DRAG_ZOOM_IN);
                    break;
                case DRAG_ZOOM_OUT:
                    handleDragZoom(deviceForPlatform, channelId, rootElement, request, DeviceControlType.DRAG_ZOOM_OUT);
                    break;
                case HOME_POSITION:
                    handleHomePositionCmd(deviceForPlatform, channelId, rootElement, request, DeviceControlType.HOME_POSITION);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 处理云台指令
     *
     * @param device      设备
     * @param channelId   通道id
     * @param rootElement
     * @param request
     */
    private void handlePtzCmd(Device device, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.fronEndCmd(device, channelId, cmdString,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 云台/前端: {}", e.getMessage());
        }
    }

    /**
     * 处理强制关键帧
     *
     * @param device    设备
     * @param channelId 通道id
     */
    private void handleIFameCmd(Device device, SIPRequest request, String channelId) {
        try {
            cmder.iFrameCmd(device, channelId);
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 强制关键帧: {}", e.getMessage());
        }
    }

    /**
     * 处理重启命令
     *
     * @param device 设备信息
     */
    private void handleTeleBootCmd(Device device, SIPRequest request) {
        try {
            cmder.teleBootCmd(device);
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 重启: {}", e.getMessage());
        }

    }

    /**
     * 处理拉框控制***
     *
     * @param device      设备信息
     * @param channelId   通道id
     * @param rootElement 根节点
     * @param type        消息类型
     */
    private void handleDragZoom(Device device, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        try {
            DragZoomRequest dragZoomRequest = loadElement(rootElement, DragZoomRequest.class);
            DragZoomRequest.DragZoom dragZoom = dragZoomRequest.getDragZoomIn();
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
            cmder.dragZoomCmd(device, channelId, cmdXml.toString());
            responseAck(request, Response.OK);
        } catch (Exception e) {
            log.error("[命令发送失败] 拉框控制: {}", e.getMessage());
        }

    }

    /**
     * 处理看守位命令***
     *
     * @param device      设备信息
     * @param channelId   通道id
     * @param rootElement 根节点
     * @param request     请求信息
     * @param type        消息类型
     */
    private void handleHomePositionCmd(Device device, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        try {
            HomePositionRequest homePosition = loadElement(rootElement, HomePositionRequest.class);
            //获取整个消息主体，我们只需要修改请求头即可
            HomePositionRequest.HomePosition info = homePosition.getHomePosition();
            cmder.homePositionCmd(device, channelId, !"0".equals(info.getEnabled()), Integer.parseInt(info.getResetTime()), Integer.parseInt(info.getPresetIndex()),
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (Exception e) {
            log.error("[命令发送失败] 看守位设置: {}", e.getMessage());
        }
    }

    /**
     * 处理告警消息***
     *
     * @param device      设备信息
     * @param rootElement 根节点
     * @param request     请求信息
     */
    private void handleAlarmCmd(Device device, Element rootElement, SIPRequest request) {
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
            cmder.alarmCmd(device, alarmMethod, alarmType,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 告警消息: {}", e.getMessage());
        }
    }

    /**
     * 处理录像控制
     *
     * @param device      设备信息
     * @param channelId   通道id
     * @param rootElement 根节点
     * @param request     请求信息
     * @param type        消息类型
     */
    private void handleRecordCmd(Device device, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.recordCmd(device, channelId, cmdString,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像控制: {}", e.getMessage());
        }
    }

    /**
     * 处理报警布防/撤防命令
     *
     * @param device      设备信息
     * @param rootElement 根节点
     * @param request     请求信息
     * @param type        消息类型
     */
    private void handleGuardCmd(Device device, Element rootElement, SIPRequest request, DeviceControlType type) {
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.guardCmd(device, cmdString,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 布防/撤防命令: {}", e.getMessage());
        }
    }


    /**
     * 错误响应处理
     *
     * @param request     请求
     * @param eventResult 响应结构
     */
    private void onError(SIPRequest request, SipSubscribe.EventResult eventResult) {
        // 失败的回复
        try {
            responseAck(request, eventResult.statusCode, eventResult.msg);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }

    /**
     * 成功响应处理
     *
     * @param request     请求
     * @param eventResult 响应结构
     */
    private void onOk(SIPRequest request, SipSubscribe.EventResult eventResult) {
        // 成功的回复
        try {
            responseAck(request, eventResult.statusCode);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }
}
