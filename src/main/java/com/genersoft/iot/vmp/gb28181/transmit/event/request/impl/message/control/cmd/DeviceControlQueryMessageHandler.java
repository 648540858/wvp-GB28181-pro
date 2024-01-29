package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.cmd;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.bean.command.CommandType;
import com.genersoft.iot.vmp.gb28181.bean.command.ICommandInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.ControlMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.IResourceService;
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


@Component
public class DeviceControlQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceControlQueryMessageHandler.class);
    private final String cmdType = "DeviceControl";

    @Autowired
    private ControlMessageHandler controlMessageHandler;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private Map<String, IResourceService> resourceServiceMap;

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
        String channelId = XmlUtil.getText(rootElement, "DeviceID");
        // 远程启动功能
        if (!ObjectUtils.isEmpty(XmlUtil.getText(rootElement, "TeleBoot"))) {
            logger.warn("[国标级联]收到平台的远程启动命令， 不处理");
            try {
                responseAck(request, Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
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
            IResourceService resourceService = resourceServiceMap.get(commonGbChannel.getType());
            if (resourceService == null) {
                try {
                    responseAck(request, Response.FORBIDDEN);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
                }
                return;
            }
            switch (deviceControlType) {
                case PTZ:
                    handlePtzCmd(commonGbChannel, resourceService, rootElement, request);
                    break;
                case ALARM:
                    handleAlarmCmd(commonGbChannel, resourceService, rootElement, request);
                    break;
                case GUARD:
                    handleGuardCmd(commonGbChannel, resourceService, rootElement, request);
                    break;
                case RECORD:
                    handleRecordCmd(commonGbChannel, resourceService, rootElement, request);
                    break;
                case I_FRAME:
                    handleIFameCmd(commonGbChannel, resourceService, request);
                    break;
                case TELE_BOOT:
                    handleTeleBootCmd(commonGbChannel, resourceService, request);
                    break;
                case DRAG_ZOOM_IN:
                    handleDragZoom(commonGbChannel, resourceService, rootElement, request, true);
                    break;
                case DRAG_ZOOM_OUT:
                    handleDragZoom(commonGbChannel, resourceService, rootElement, request, false);
                    break;
                case HOME_POSITION:
                    handleHomePositionCmd(commonGbChannel, resourceService, rootElement, request);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 处理云台指令
     */
    private void handlePtzCmd(CommonGbChannel commonGbChannel, IResourceService resourceService,  Element rootElement, SIPRequest request) {
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
        }
        String cmdString = XmlUtil.getText(rootElement, DeviceControlType.PTZ.getVal());
        // 解析云台控制参数
        ICommandInfo commandInfo = ControlCommand.analysisCommand(cmdString);
        if (commandInfo == null || !commandInfo.getType().equals(CommandType.PTZ)) {
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
    }

    /**
     * 处理强制关键帧
     */
    private void handleIFameCmd(CommonGbChannel commonGbChannel, IResourceService resourceService, SIPRequest request) {
        logger.info("\r\n[强制关键帧] channelID： {} ", commonGbChannel.getCommonGbDeviceID());
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
        }
        resourceService.setIFame(commonGbChannel);
    }



    /**
     * 处理重启命令
     *
     */
    private void handleTeleBootCmd(CommonGbChannel commonGbChannel, IResourceService resourceService, SIPRequest request) {
        logger.info("\r\n[重启设备] channelID： {} ", commonGbChannel.getCommonGbDeviceID());
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[重启设备] 错误信息: {}", e.getMessage());
        }
        resourceService.setTeleBoot(commonGbChannel);
    }

    /**
     * 处理拉框控制
     */
    private void handleDragZoom(CommonGbChannel commonGbChannel, IResourceService resourceService, Element rootElement, SIPRequest request, boolean isIn) {
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[拉框控制] 错误信息: {}", e.getMessage());
        }

        try {
            DragZoomRequest dragZoomRequest = XmlUtil.loadElement(rootElement, DragZoomRequest.class);
            DragZoomRequest.DragZoom dragZoom;
            if (isIn) {
                logger.info("\r\n[拉框放大] channelID： {}; 参数： {}", commonGbChannel.getCommonGbDeviceID(),
                        dragZoomRequest.getDragZoomIn().toString());
                dragZoom = dragZoomRequest.getDragZoomIn();
            }else {
                logger.info("\r\n[拉框缩小] channelID： {}; 参数： {} ", commonGbChannel.getCommonGbDeviceID(),
                        dragZoomRequest.getDragZoomIn().toString());
                dragZoom = dragZoomRequest.getDragZoomOut();
            }
            resourceService.dragZoom(commonGbChannel, dragZoom, isIn);
        } catch (Exception e) {
            logger.error("[命令发送失败] 拉框控制: {}", e.getMessage());
        }

    }

    /**
     * 处理看守位命令
     */
    private void handleHomePositionCmd(CommonGbChannel commonGbChannel,  IResourceService resourceService, Element rootElement, SIPRequest request) {
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[拉框控制] 错误信息: {}", e.getMessage());
        }

        try {
            HomePositionRequest homePosition = XmlUtil.loadElement(rootElement, HomePositionRequest.class);
            //获取整个消息主体，我们只需要修改请求头即可
            HomePositionRequest.HomePosition info = homePosition.getHomePosition();
            if (info.getEnabled() == null) {
                return;
            }
            resourceService.setHomePosition(commonGbChannel, info.getEnabled().equals("1"),
                    info.getResetTime() != null ? Integer.parseInt(info.getResetTime()): null,
                    info.getPresetIndex() != null ? Integer.parseInt(info.getPresetIndex()): null);

        } catch (Exception e) {
            logger.error("[命令发送失败] 看守位设置: {}", e.getMessage());
        }
    }

    /**
     * 处理告警消息
     */
    private void handleAlarmCmd(CommonGbChannel commonGbChannel, IResourceService resourceService, Element rootElement, SIPRequest request) {
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 回复: {}", e.getMessage());
        }

        //告警方法
        Integer alarmMethod = null;
        //告警类型
        Integer alarmType = null;
        List<Element> info = rootElement.elements("Info");
        if (info != null) {
            for (Element element : info) {
                String alarmMethodStr = XmlUtil.getText(element, "AlarmMethod");
                if (alarmMethodStr != null) {
                    alarmMethod = Integer.parseInt(alarmMethodStr);
                }
                String alarmTypeStr = XmlUtil.getText(element, "AlarmType");
                if (alarmTypeStr != null) {
                    alarmType = Integer.parseInt(alarmTypeStr);
                }
            }
        }

        logger.info("\r\n[报警复位]: alarmMethod： {} alarmType： {}", alarmMethod, alarmType);

        resourceService.resetAlarm(commonGbChannel, alarmMethod, alarmType);
    }

    /**
     * 处理录像控制
     */
    private void handleRecordCmd(CommonGbChannel commonGbChannel, IResourceService resourceService, Element rootElement, SIPRequest request) {
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = XmlUtil.getText(rootElement, DeviceControlType.RECORD.getVal());
        Boolean isRecord = null;
        if (cmdString.equalsIgnoreCase("Record")) {
            isRecord = true;
        }else if (cmdString.equalsIgnoreCase("StopRecord")) {
            isRecord = false;
        }else {
            logger.info("\r\n[录像控制] 解析失败： {} ", commonGbChannel.getCommonGbDeviceID());
            return;
        }
        if (isRecord) {
            logger.info("\r\n[录像控制] 开始录像: channelId： {} ", commonGbChannel.getCommonGbDeviceID());
        }else {
            logger.info("\r\n[录像控制] 停止录像： {} ", commonGbChannel.getCommonGbDeviceID());
        }

        resourceService.setRecord(commonGbChannel, isRecord);
    }



    /**
     * 处理报警布防/撤防命令
     */
    private void handleGuardCmd(CommonGbChannel commonGbChannel, IResourceService resourceService, Element rootElement, SIPRequest request) {
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
        }
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = XmlUtil.getText(rootElement, DeviceControlType.GUARD.getVal());
        boolean setGuard;
        if (cmdString.equalsIgnoreCase("Record")) {
            setGuard = true;
        }else if (cmdString.equalsIgnoreCase("StopRecord")) {
            setGuard = false;
        }else {
            return;
        }
        if (setGuard) {
            logger.info("\r\n[报警布防]: channelId： {} ", commonGbChannel.getCommonGbDeviceID());
        }else {
            logger.info("\r\n[报警撤防]: channelId： {} ", commonGbChannel.getCommonGbDeviceID());
        }

        resourceService.setGuard(commonGbChannel, setGuard);
    }
}
