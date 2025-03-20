package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.gb28181.bean.SipMsgInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 状态信息(心跳)报送
 */
@Slf4j
@Component
public class KeepaliveNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {


    private final static String cmdType = "Keepalive";

    private final ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[心跳] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
            return;
        }
        taskQueue.offer(new SipMsgInfo(evt, device, rootElement));
    }

    @Scheduled(fixedDelay = 100)
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<SipMsgInfo> handlerCatchDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            SipMsgInfo poll = taskQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        for (SipMsgInfo sipMsgInfo : handlerCatchDataList) {
            if (sipMsgInfo == null) {
                continue;
            }
            RequestEvent evt = sipMsgInfo.getEvt();
            // 回复200 OK
            try {
                responseAck((SIPRequest) evt.getRequest(), Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 心跳回复: {}", e.getMessage());
            }
            Device device = sipMsgInfo.getDevice();
            SIPRequest request = (SIPRequest) evt.getRequest();
//            if (!ObjectUtils.isEmpty(device.getKeepaliveTime()) && DateUtil.getDifferenceForNow(device.getKeepaliveTime()) <= 3000L) {
//                log.info("[收到心跳] 心跳发送过于频繁，已忽略 device: {}, callId: {}", device.getDeviceId(), request.getCallIdHeader().getCallId());
//                return;
//            }

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request, userSetting.getSipUseSourceIpAsRemoteAddress());
            if (!device.getIp().equalsIgnoreCase(remoteAddressInfo.getIp()) || device.getPort() != remoteAddressInfo.getPort()) {
                log.info("[收到心跳] 地址变化, {}({}), {}:{}->{}", device.getName(), device.getDeviceId(), remoteAddressInfo.getIp(), remoteAddressInfo.getPort(), request.getLocalAddress().getHostAddress());
                device.setPort(remoteAddressInfo.getPort());
                device.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));
                device.setIp(remoteAddressInfo.getIp());
                device.setLocalIp(request.getLocalAddress().getHostAddress());
                // 设备地址变化会引起目录订阅任务失效，需要重新添加
                if (device.getSubscribeCycleForCatalog() > 0) {
                    deviceService.removeCatalogSubscribe(device, result -> {
                        deviceService.addCatalogSubscribe(device);
                    });
                }
            }

            device.setKeepaliveTime(DateUtil.getNow());

            if (device.isOnLine()) {
                deviceService.updateDevice(device);
            } else {
                if (userSetting.getGbDeviceOnline() == 1) {
                    // 对于已经离线的设备判断他的注册是否已经过期
                    device.setOnLine(true);
                    device.setRegisterTime(DateUtil.getNow());
                    deviceService.online(device, null);
                }
            }
            // 刷新过期任务
            String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + device.getDeviceId();
            // 如果三次心跳失败，则设置设备离线
            dynamicTask.startDelay(registerExpireTaskKey, () -> deviceService.offline(device.getDeviceId(), "三次心跳超时"),
                    device.getHeartBeatInterval() * 1000 * device.getHeartBeatCount());

        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {
        // 个别平台保活不回复200OK会判定离线
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 心跳回复: {}", e.getMessage());
        }
    }
}
