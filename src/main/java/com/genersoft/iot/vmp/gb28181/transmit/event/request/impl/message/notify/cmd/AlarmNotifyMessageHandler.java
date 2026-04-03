package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
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
 * 报警事件的处理，参考：9.4
 */
@Slf4j
@Component
public class AlarmNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "Alarm";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    private final ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[Alarm] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
            return;
        }
        // 回复200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 报警通知回复: {}", e.getMessage());
        }
        taskQueue.offer(new SipMsgInfo(evt, device, rootElement));
    }

    @Scheduled(fixedDelay = 200)
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
        List<DeviceAlarmNotify> deviceAlarmList = new ArrayList<>();
        for (SipMsgInfo sipMsgInfo : handlerCatchDataList) {
            if (sipMsgInfo == null || sipMsgInfo.getDevice() == null) {
                continue;
            }
            RequestEvent evt = sipMsgInfo.getEvt();

            try {
                DeviceAlarmNotify deviceAlarmNotify = DeviceAlarmNotify.fromXml(sipMsgInfo.getRootElement());
                Device device = sipMsgInfo.getDevice();
                if (log.isDebugEnabled()) {
                    log.debug("[收到报警通知]设备：{}， 内容：{}", device.getDeviceId(), JSON.toJSONString(deviceAlarmNotify));
                }
                deviceAlarmNotify.setDeviceId(device.getDeviceId());
                deviceAlarmNotify.setDeviceName(device.getName());
                if (deviceAlarmNotify.getAlarmMethod() != null && deviceAlarmNotify.getAlarmMethod() == DeviceAlarmMethod.GPS.getVal()) {
                    DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), deviceAlarmNotify.getChannelId());
                    if (deviceChannel == null) {
                        log.warn("[解析报警消息] 未找到通道：{}/{}", device.getDeviceId(), deviceAlarmNotify.getChannelId());
                    } else {
                        MobilePosition mobilePosition = new MobilePosition();
                        mobilePosition.setCreateTime(DateUtil.getNow());
                        mobilePosition.setDeviceId(device.getDeviceId());
                        mobilePosition.setChannelId(deviceChannel.getId());
                        mobilePosition.setChannelDeviceId(deviceChannel.getDeviceId());
                        mobilePosition.setTime(deviceAlarmNotify.getAlarmTime());
                        mobilePosition.setLongitude(deviceAlarmNotify.getLongitude());
                        mobilePosition.setLatitude(deviceAlarmNotify.getLatitude());
                        mobilePosition.setReportSource("GPS Alarm");

                        // 更新device channel 的经纬度
                        deviceChannel.setLongitude(mobilePosition.getLongitude());
                        deviceChannel.setLatitude(mobilePosition.getLatitude());
                        deviceChannel.setGpsTime(mobilePosition.getTime());

                        deviceChannelService.updateChannelGPS(device, deviceChannel, mobilePosition);
                    }
                }

                // 作者自用判断，其他小伙伴需要此消息可以自行修改，但是不要提在pr里
                if (deviceAlarmNotify.getAlarmMethod() != null
                        && DeviceAlarmMethod.Other.getVal() == deviceAlarmNotify.getAlarmMethod()) {
                    // 发送给平台的报警信息。 发送redis通知
                    log.info("[发送给平台的报警信息]内容：{}", JSONObject.toJSONString(deviceAlarmNotify));
                    AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
                    alarmChannelMessage.setAlarmSn(deviceAlarmNotify.getAlarmMethod());
                    alarmChannelMessage.setAlarmDescription(deviceAlarmNotify.getAlarmDescription());
                    alarmChannelMessage.setAlarmType(deviceAlarmNotify.getAlarmType());
                    alarmChannelMessage.setGbId(deviceAlarmNotify.getChannelId());
                    redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
                    continue;
                }

                if (redisCatchStorage.deviceIsOnline(sipMsgInfo.getDevice().getDeviceId())) {
                    deviceAlarmList.add(deviceAlarmNotify);
                }
            } catch (Exception e) {
                log.error("未处理的异常 ", e);
                log.warn("[收到报警通知] 发现未处理的异常, {}\r\n{}", e.getMessage(), evt.getRequest());
            }
        }
        if (deviceAlarmList.isEmpty()) {
            return;
        }
        publisher.deviceAlarmEventPublish(deviceAlarmList);
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {
        log.info("收到来自平台[{}]的报警通知", parentPlatform.getServerGBId());
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 报警通知回复: {}", e.getMessage());
        }
        Element deviceIdElement = rootElement.element("DeviceID");
        String channelId = deviceIdElement.getText();
        DeviceAlarmNotify deviceAlarmNotify = DeviceAlarmNotify.fromXml(rootElement);
        deviceAlarmNotify.setDeviceId(parentPlatform.getServerGBId());
        deviceAlarmNotify.setDeviceName(parentPlatform.getName());
        deviceAlarmNotify.setChannelId(channelId);

        if (channelId.equals(parentPlatform.getDeviceGBId())) {
            // 发送给平台的报警信息。 发送redis通知
            AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
            alarmChannelMessage.setAlarmSn(deviceAlarmNotify.getAlarmMethod());
            alarmChannelMessage.setAlarmDescription(deviceAlarmNotify.getAlarmDescription());
            alarmChannelMessage.setGbId(channelId);
            alarmChannelMessage.setAlarmType(deviceAlarmNotify.getAlarmType());
            redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
        }
    }
}
