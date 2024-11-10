package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

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
    private IDeviceAlarmService deviceAlarmService;

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
        for (SipMsgInfo sipMsgInfo : handlerCatchDataList) {
            if (sipMsgInfo == null) {
                continue;
            }
            RequestEvent evt = sipMsgInfo.getEvt();
            // 回复200 OK
            try {
                responseAck((SIPRequest) evt.getRequest(), Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 报警通知回复: {}", e.getMessage());
            }
            try {
                Device device = sipMsgInfo.getDevice();
                Element deviceIdElement = sipMsgInfo.getRootElement().element("DeviceID");
                String channelId = deviceIdElement.getText();

                DeviceAlarm deviceAlarm = new DeviceAlarm();
                deviceAlarm.setCreateTime(DateUtil.getNow());
                deviceAlarm.setDeviceId(sipMsgInfo.getDevice().getDeviceId());
                deviceAlarm.setDeviceName(sipMsgInfo.getDevice().getName());
                deviceAlarm.setChannelId(channelId);
                deviceAlarm.setAlarmPriority(getText(sipMsgInfo.getRootElement(), "AlarmPriority"));
                deviceAlarm.setAlarmMethod(getText(sipMsgInfo.getRootElement(), "AlarmMethod"));
                String alarmTime = XmlUtil.getText(sipMsgInfo.getRootElement(), "AlarmTime");
                if (alarmTime == null) {
                    continue;
                }
                deviceAlarm.setAlarmTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(alarmTime));
                String alarmDescription = getText(sipMsgInfo.getRootElement(), "AlarmDescription");
                if (alarmDescription == null) {
                    deviceAlarm.setAlarmDescription("");
                } else {
                    deviceAlarm.setAlarmDescription(alarmDescription);
                }
                String longitude = getText(sipMsgInfo.getRootElement(), "Longitude");
                if (longitude != null && NumericUtil.isDouble(longitude)) {
                    deviceAlarm.setLongitude(Double.parseDouble(longitude));
                } else {
                    deviceAlarm.setLongitude(0.00);
                }
                String latitude = getText(sipMsgInfo.getRootElement(), "Latitude");
                if (latitude != null && NumericUtil.isDouble(latitude)) {
                    deviceAlarm.setLatitude(Double.parseDouble(latitude));
                } else {
                    deviceAlarm.setLatitude(0.00);
                }

                if (!ObjectUtils.isEmpty(deviceAlarm.getAlarmMethod()) && deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.GPS.getVal() + "")) {
                    DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), channelId);
                    if (deviceChannel == null) {
                        log.warn("[解析报警消息] 未找到通道：{}/{}", device.getDeviceId(), channelId);
                    } else {
                        MobilePosition mobilePosition = new MobilePosition();
                        mobilePosition.setCreateTime(DateUtil.getNow());
                        mobilePosition.setDeviceId(deviceAlarm.getDeviceId());
                        mobilePosition.setChannelId(deviceChannel.getId());
                        mobilePosition.setTime(deviceAlarm.getAlarmTime());
                        mobilePosition.setLongitude(deviceAlarm.getLongitude());
                        mobilePosition.setLatitude(deviceAlarm.getLatitude());
                        mobilePosition.setReportSource("GPS Alarm");

                        // 更新device channel 的经纬度
                        deviceChannel.setLongitude(mobilePosition.getLongitude());
                        deviceChannel.setLatitude(mobilePosition.getLatitude());
                        deviceChannel.setGpsTime(mobilePosition.getTime());

                        deviceChannelService.updateChannelGPS(device, deviceChannel, mobilePosition);
                    }
                }
                if (!ObjectUtils.isEmpty(deviceAlarm.getDeviceId())) {
                    if (deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.Video.getVal() + "")) {
                        deviceAlarm.setAlarmType(getText(sipMsgInfo.getRootElement().element("Info"), "AlarmType"));
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("[收到报警通知]设备：{}， 内容：{}", device.getDeviceId(), JSON.toJSONString(deviceAlarm));
                }
                // 作者自用判断，其他小伙伴需要此消息可以自行修改，但是不要提在pr里
                if (DeviceAlarmMethod.Other.getVal() == Integer.parseInt(deviceAlarm.getAlarmMethod())) {
                    // 发送给平台的报警信息。 发送redis通知
                    log.info("[发送给平台的报警信息]内容：{}", JSONObject.toJSONString(deviceAlarm));
                    AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
                    if (deviceAlarm.getAlarmMethod() != null) {
                        alarmChannelMessage.setAlarmSn(Integer.parseInt(deviceAlarm.getAlarmMethod()));
                    }
                    alarmChannelMessage.setAlarmDescription(deviceAlarm.getAlarmDescription());
                    if (deviceAlarm.getAlarmType() != null) {
                        alarmChannelMessage.setAlarmType(Integer.parseInt(deviceAlarm.getAlarmType()));
                    }
                    alarmChannelMessage.setGbId(channelId);
                    redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
                    continue;
                }

                log.debug("存储报警信息、报警分类");
                // 存储报警信息、报警分类
                if (sipConfig.isAlarm()) {
                    deviceAlarmService.add(deviceAlarm);
                }

                if (redisCatchStorage.deviceIsOnline(sipMsgInfo.getDevice().getDeviceId())) {
                    publisher.deviceAlarmEventPublish(deviceAlarm);
                }
            } catch (Exception e) {
                log.error("未处理的异常 ", e);
                log.warn("[收到报警通知] 发现未处理的异常, {}\r\n{}", e.getMessage(), evt.getRequest());
            }
        }
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


        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setCreateTime(DateUtil.getNow());
        deviceAlarm.setDeviceId(parentPlatform.getServerGBId());
        deviceAlarm.setDeviceName(parentPlatform.getName());
        deviceAlarm.setChannelId(channelId);
        deviceAlarm.setAlarmPriority(getText(rootElement, "AlarmPriority"));
        deviceAlarm.setAlarmMethod(getText(rootElement, "AlarmMethod"));
        String alarmTime = XmlUtil.getText(rootElement, "AlarmTime");
        if (alarmTime == null) {
            return;
        }
        deviceAlarm.setAlarmTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(alarmTime));
        String alarmDescription = getText(rootElement, "AlarmDescription");
        if (alarmDescription == null) {
            deviceAlarm.setAlarmDescription("");
        } else {
            deviceAlarm.setAlarmDescription(alarmDescription);
        }
        String longitude = getText(rootElement, "Longitude");
        if (longitude != null && NumericUtil.isDouble(longitude)) {
            deviceAlarm.setLongitude(Double.parseDouble(longitude));
        } else {
            deviceAlarm.setLongitude(0.00);
        }
        String latitude = getText(rootElement, "Latitude");
        if (latitude != null && NumericUtil.isDouble(latitude)) {
            deviceAlarm.setLatitude(Double.parseDouble(latitude));
        } else {
            deviceAlarm.setLatitude(0.00);
        }

        if (!ObjectUtils.isEmpty(deviceAlarm.getAlarmMethod())) {

            if (deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.Video.getVal() + "")) {
                deviceAlarm.setAlarmType(getText(rootElement.element("Info"), "AlarmType"));
            }
        }

        if (channelId.equals(parentPlatform.getDeviceGBId())) {
            // 发送给平台的报警信息。 发送redis通知
            AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
            if (deviceAlarm.getAlarmMethod() != null) {
                alarmChannelMessage.setAlarmSn(Integer.parseInt(deviceAlarm.getAlarmMethod()));
            }
            alarmChannelMessage.setAlarmDescription(deviceAlarm.getAlarmDescription());
            alarmChannelMessage.setGbId(channelId);
            if (deviceAlarm.getAlarmType() != null) {
                alarmChannelMessage.setAlarmType(Integer.parseInt(deviceAlarm.getAlarmType()));
            }
            redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
        }
    }
}
