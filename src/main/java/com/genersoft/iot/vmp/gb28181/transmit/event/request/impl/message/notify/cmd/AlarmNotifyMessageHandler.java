package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
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

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 报警事件的处理，参考：9.4
 */
@Component
public class AlarmNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(AlarmNotifyMessageHandler.class);
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
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceAlarmService deviceAlarmService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    private ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        logger.info("[收到报警通知]设备：{}", device.getDeviceId());
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(new SipMsgInfo(evt, device, rootElement));
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 报警通知回复: {}", e.getMessage());
        }
        if (isEmpty) {
            taskExecutor.execute(() -> {
                logger.info("[处理报警通知]待处理数量：{}", taskQueue.size() );
                while (!taskQueue.isEmpty()) {
                    try {
                        SipMsgInfo sipMsgInfo = taskQueue.poll();

                        Element deviceIdElement = sipMsgInfo.getRootElement().element("DeviceID");
                        String channelId = deviceIdElement.getText().toString();

                        DeviceAlarm deviceAlarm = new DeviceAlarm();
                        deviceAlarm.setCreateTime(DateUtil.getNow());
                        deviceAlarm.setDeviceId(sipMsgInfo.getDevice().getDeviceId());
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

                        if (!ObjectUtils.isEmpty(deviceAlarm.getAlarmMethod())) {
                            if ( deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.GPS.getVal() + "")) {
                                MobilePosition mobilePosition = new MobilePosition();
                                mobilePosition.setCreateTime(DateUtil.getNow());
                                mobilePosition.setDeviceId(deviceAlarm.getDeviceId());
                                mobilePosition.setChannelId(channelId);
                                mobilePosition.setTime(deviceAlarm.getAlarmTime());
                                mobilePosition.setLongitude(deviceAlarm.getLongitude());
                                mobilePosition.setLatitude(deviceAlarm.getLatitude());
                                mobilePosition.setReportSource("GPS Alarm");

                                // 更新device channel 的经纬度
                                DeviceChannel deviceChannel = new DeviceChannel();
                                deviceChannel.setDeviceId(sipMsgInfo.getDevice().getDeviceId());
                                deviceChannel.setChannelId(channelId);
                                deviceChannel.setLongitude(mobilePosition.getLongitude());
                                deviceChannel.setLatitude(mobilePosition.getLatitude());
                                deviceChannel.setGpsTime(mobilePosition.getTime());

                                deviceChannel = deviceChannelService.updateGps(deviceChannel, sipMsgInfo.getDevice());

                                mobilePosition.setLongitudeWgs84(deviceChannel.getLongitudeWgs84());
                                mobilePosition.setLatitudeWgs84(deviceChannel.getLatitudeWgs84());
                                mobilePosition.setLongitudeGcj02(deviceChannel.getLongitudeGcj02());
                                mobilePosition.setLatitudeGcj02(deviceChannel.getLatitudeGcj02());

                                if (userSetting.getSavePositionHistory()) {
                                    storager.insertMobilePosition(mobilePosition);
                                }
                                storager.updateChannelPosition(deviceChannel);

                                // 发送redis消息。 通知位置信息的变化
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
                                jsonObject.put("serial", deviceChannel.getDeviceId());
                                jsonObject.put("code", deviceChannel.getChannelId());
                                jsonObject.put("longitude", mobilePosition.getLongitude());
                                jsonObject.put("latitude", mobilePosition.getLatitude());
                                jsonObject.put("altitude", mobilePosition.getAltitude());
                                jsonObject.put("direction", mobilePosition.getDirection());
                                jsonObject.put("speed", mobilePosition.getSpeed());
                                redisCatchStorage.sendMobilePositionMsg(jsonObject);
                            }
                        }
                        if (!ObjectUtils.isEmpty(deviceAlarm.getDeviceId())) {
                            if (deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.Video.getVal() + "")) {
                                deviceAlarm.setAlarmType(getText(sipMsgInfo.getRootElement().element("Info"), "AlarmType"));
                            }
                        }
                        logger.info("[收到报警通知]内容：{}", JSON.toJSONString(deviceAlarm));
                        // 作者自用判断，其他小伙伴需要此消息可以自行修改，但是不要提在pr里
                        if (DeviceAlarmMethod.Other.getVal() == Integer.parseInt(deviceAlarm.getAlarmMethod())) {
                            // 发送给平台的报警信息。 发送redis通知
                            logger.info("[发送给平台的报警信息]内容：{}", JSONObject.toJSONString(deviceAlarm));
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

                        logger.debug("存储报警信息、报警分类");
                        // 存储报警信息、报警分类
                        if (sipConfig.isAlarm()) {
                            deviceAlarmService.add(deviceAlarm);
                        }

                        if (redisCatchStorage.deviceIsOnline(sipMsgInfo.getDevice().getDeviceId())) {
                            publisher.deviceAlarmEventPublish(deviceAlarm);
                        }
                    }catch (Exception e) {
                        logger.error("未处理的异常 ", e);
                        logger.warn("[收到报警通知] 发现未处理的异常, {}\r\n{}",e.getMessage(), evt.getRequest());
                    }
                }
            });
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {
        logger.info("收到来自平台[{}]的报警通知", parentPlatform.getServerGBId());
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 国标级联 报警通知回复: {}", e.getMessage());
        }
        Element deviceIdElement = rootElement.element("DeviceID");
        String channelId = deviceIdElement.getText().toString();


        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setCreateTime(DateUtil.getNow());
        deviceAlarm.setDeviceId(parentPlatform.getServerGBId());
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
