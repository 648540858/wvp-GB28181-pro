package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.task.deviceStatus.DeviceStatusTaskRunner;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.IpPortUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 状态信息(心跳)报送
 */
@Slf4j
@Component
public class KeepaliveNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {


    private final static String cmdType = "Keepalive";

    private final BlockingQueue<Device> taskQueue = new LinkedBlockingQueue<>();

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private DeviceStatusTaskRunner statusTaskRunner;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 心跳回复: {}", e.getMessage());
        }
        SIPRequest request = (SIPRequest) evt.getRequest();

        RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request, userSetting.getSipUseSourceIpAsRemoteAddress());
        if (device.getIp() == null || !device.getIp().equalsIgnoreCase(remoteAddressInfo.getIp()) || device.getPort() != remoteAddressInfo.getPort()) {
            log.info("[收到心跳] 地址变化, {}({}), {}:{}->{}", device.getName(), device.getDeviceId(), remoteAddressInfo.getIp(), remoteAddressInfo.getPort(), request.getLocalAddress().getHostAddress());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));
            device.setIp(remoteAddressInfo.getIp());
            device.setLocalIp(request.getLocalAddress().getHostAddress());
        }

        device.setKeepaliveTime(DateUtil.getNow());

        if (device.isOnLine()) {
            taskQueue.add(device);
            long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
            statusTaskRunner.addTask(device.getDeviceId(), expiresTime + System.currentTimeMillis());
        } else {
            if (userSetting.getGbDeviceOnline() == 1) {
                // 对于已经离线的设备判断他的注册是否已经过期
                deviceService.online(device, null);
            }
        }
    }
    @Scheduled(fixedDelay = 1000, timeUnit = TimeUnit.MILLISECONDS)
    @Async
    public void executeUpdateDeviceList() {
        if (!taskQueue.isEmpty()) {
            deviceService.updateDeviceListForKeepalive(taskQueue.stream().toList());
            taskQueue.clear();
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
