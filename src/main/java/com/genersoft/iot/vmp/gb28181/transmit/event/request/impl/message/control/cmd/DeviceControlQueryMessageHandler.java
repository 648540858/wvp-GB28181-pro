package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.cmd;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.bean.command.CommandType;
import com.genersoft.iot.vmp.gb28181.bean.command.ICommandInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.ControlMessageHandler;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.*;

@Component
public class DeviceControlQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceControlQueryMessageHandler.class);
    private final String cmdType = "DeviceControl";

    @Autowired
    private ControlMessageHandler controlMessageHandler;


    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private Map<String, IResourceService> resourceServiceMap;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void afterPropertiesSet() throws Exception {
        controlMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();

        // 此处是上级发出的DeviceControl指令
        String targetGBId = ((SipURI) request.getToHeader().getAddress().getURI()).getUser();
        String channelId = getText(rootElement, "DeviceID");
        // 远程启动功能
        if (!ObjectUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
            // TODO 拒绝远程启动命令
            logger.warn("[国标级联]收到平台的远程启动命令， 不处理");
        }
        DeviceControlType deviceControlType = DeviceControlType.typeOf(rootElement);
        logger.info("[接受deviceControl命令] 命令: {}", deviceControlType);
        if (!ObjectUtils.isEmpty(deviceControlType) && !parentPlatform.getServerGBId().equals(targetGBId)) {

            CommonGbChannel commonGbChannel = platformChannelService.queryChannelByPlatformIdAndChannelDeviceId(parentPlatform.getId(), channelId);
            if (commonGbChannel == null) {
                try {
                    responseAck(request, Response.NOT_FOUND);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
                }
                return;
            }

            switch (deviceControlType) {
                case PTZ:
                    handlePtzCmd(commonGbChannel, rootElement, request, DeviceControlType.PTZ);
                    break;
//                case ALARM:
//                    handleAlarmCmd(deviceForPlatform, rootElement, request);
//                    break;
//                case GUARD:
//                    handleGuardCmd(deviceForPlatform, rootElement, request, DeviceControlType.GUARD);
//                    break;
//                case RECORD:
//                    handleRecordCmd(deviceForPlatform, channelId, rootElement, request, DeviceControlType.RECORD);
//                    break;
//                case I_FRAME:
//                    handleIFameCmd(deviceForPlatform, request, channelId);
//                    break;
//                case TELE_BOOT:
//                    handleTeleBootCmd(deviceForPlatform, request);
//                    break;
//                case DRAG_ZOOM_IN:
//                    handleDragZoom(deviceForPlatform, channelId, rootElement, request, DeviceControlType.DRAG_ZOOM_IN);
//                    break;
//                case DRAG_ZOOM_OUT:
//                    handleDragZoom(deviceForPlatform, channelId, rootElement, request, DeviceControlType.DRAG_ZOOM_OUT);
//                    break;
//                case HOME_POSITION:
//                    handleHomePositionCmd(deviceForPlatform, channelId, rootElement, request, DeviceControlType.HOME_POSITION);
//                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 处理云台指令
     */
    private void handlePtzCmd(CommonGbChannel commonGbChannel, Element rootElement, SIPRequest request, DeviceControlType type) {
        IResourceService resourceService = resourceServiceMap.get(commonGbChannel.getType());
        if (resourceService == null) {
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }

        String cmdString = getText(rootElement, type.getVal());
        // 解析云台控制参数
        ICommandInfo commandInfo = ControlCommand.analysisCommand(cmdString);
        if (commandInfo == null || !commandInfo.getType().equals(CommandType.PTZ)) {
            try {
                responseAck(request, Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        PTZCommand ptzCommand = (PTZCommand)commandInfo;
        logger.info("\r\n[云台控制]: 命令： {}" +
                        " \r\n镜头变倍: 放大： {}，缩小： {}， " +
                        " \r\n方向控制： 上： {}， 下： {}， 左： {}，右： {}" +
                        " \r\n平控制速度相对值: {}, 垂直控制速度相对值: {}, 变倍控制速度相对值: {}",
                cmdString,
                ptzCommand.isIn(), ptzCommand.isOut(),
                ptzCommand.isUp(), ptzCommand.isDown(), ptzCommand.isLeft(), ptzCommand.isRight(),
                ptzCommand.getxSpeed(), ptzCommand.getySpeed(), ptzCommand.getzSpeed());

        resourceService.ptzControl(commonGbChannel, ptzCommand);

//        System.out.println();
//        byte[] bytes = cmdString.getBytes();
//        System.out.println(cmdString);
//        for (byte aByte : bytes) {
//            System.out.print(aByte);
//            System.out.print(" ");
//        }
//        System.out.println(" ");
//        try {
//            cmder.fronEndCmd(device, channelId, cmdString,
//                    errorResult -> onError(request, errorResult),
//                    okResult -> onOk(request, okResult));
//        } catch (InvalidArgumentException | SipException | ParseException e) {
//            logger.error("[命令发送失败] 云台/前端: {}", e.getMessage());
//        }
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
            logger.error("[命令发送失败] 强制关键帧: {}", e.getMessage());
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
            logger.error("[命令发送失败] 重启: {}", e.getMessage());
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
            logger.error("[命令发送失败] 拉框控制: {}", e.getMessage());
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
            if (info.getEnabled() == null) {
                return;
            }
            cmder.homePositionCmd(device, channelId, info.getEnabled().equals("1"),
                    info.getResetTime() != null ? Integer.parseInt(info.getResetTime()): null,
                    info.getPresetIndex() != null ? Integer.parseInt(info.getPresetIndex()): null,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (Exception e) {
            logger.error("[命令发送失败] 看守位设置: {}", e.getMessage());
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
        Integer alarmMethod = null;
        //告警类型
        Integer alarmType = null;
        List<Element> info = rootElement.elements("Info");
        if (info != null) {
            for (Element element : info) {
                String alarmMethodStr = getText(element, "AlarmMethod");
                if (alarmMethodStr != null) {
                    alarmMethod = Integer.parseInt(alarmMethodStr);
                }
                String alarmTypeStr = getText(element, "AlarmType");
                if (alarmTypeStr != null) {
                    alarmType = Integer.parseInt(alarmTypeStr);
                }
            }
        }
        try {
            cmder.alarmCmd(device, alarmMethod, alarmType,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 告警消息: {}", e.getMessage());
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
        Boolean isRecord = null;
        if (cmdString.equalsIgnoreCase("Record")) {
            isRecord = true;
        }else if (cmdString.equalsIgnoreCase("StopRecord")) {
            isRecord = false;
        }else {
            return;
        }
        try {
            cmder.recordCmd(device, channelId, isRecord,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 录像控制: {}", e.getMessage());
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
        boolean setGuard;
        if (cmdString.equalsIgnoreCase("Record")) {
            setGuard = true;
        }else if (cmdString.equalsIgnoreCase("StopRecord")) {
            setGuard = false;
        }else {
            return;
        }
        try {
            cmder.guardCmd(device, setGuard,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 布防/撤防命令: {}", e.getMessage());
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
            logger.error("[命令发送失败] 回复: {}", e.getMessage());
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
            logger.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }
}
