package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;

import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.*;

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

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        logger.info("[收到报警通知]设备：{}", device.getDeviceId());
        // 回复200 OK
        try {
            responseAck(evt, Response.OK);
        } catch (SipException e) {
            throw new RuntimeException(e);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Element deviceIdElement = rootElement.element("DeviceID");
        String channelId = deviceIdElement.getText().toString();

        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setCreateTime(DateUtil.getNow());
        deviceAlarm.setDeviceId(device.getDeviceId());
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

        if (!StringUtils.isEmpty(deviceAlarm.getAlarmMethod())) {
            if ( deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.GPS.getVal() + "")) {
                MobilePosition mobilePosition = new MobilePosition();
                mobilePosition.setCreateTime(DateUtil.getNow());
                mobilePosition.setDeviceId(deviceAlarm.getDeviceId());
                mobilePosition.setTime(deviceAlarm.getAlarmTime());
                mobilePosition.setLongitude(deviceAlarm.getLongitude());
                mobilePosition.setLatitude(deviceAlarm.getLatitude());
                mobilePosition.setReportSource("GPS Alarm");


                // 更新device channel 的经纬度
                DeviceChannel deviceChannel = new DeviceChannel();
                deviceChannel.setDeviceId(device.getDeviceId());
                deviceChannel.setChannelId(channelId);
                deviceChannel.setLongitude(mobilePosition.getLongitude());
                deviceChannel.setLatitude(mobilePosition.getLatitude());
                deviceChannel.setGpsTime(mobilePosition.getTime());

                deviceChannel = deviceChannelService.updateGps(deviceChannel, device);

                mobilePosition.setLongitudeWgs84(deviceChannel.getLongitudeWgs84());
                mobilePosition.setLatitudeWgs84(deviceChannel.getLatitudeWgs84());
                mobilePosition.setLongitudeGcj02(deviceChannel.getLongitudeGcj02());
                mobilePosition.setLatitudeGcj02(deviceChannel.getLatitudeGcj02());

                if (userSetting.getSavePositionHistory()) {
                    storager.insertMobilePosition(mobilePosition);
                }
                storager.updateChannelPosition(deviceChannel);
            }
        }
        if (!StringUtils.isEmpty(deviceAlarm.getDeviceId())) {
            if (deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.Video.getVal() + "")) {
                deviceAlarm.setAlarmType(getText(rootElement.element("Info"), "AlarmType"));
            }
        }

        if (channelId.equals(sipConfig.getId())) {
            // 发送给平台的报警信息。 发送redis通知
            AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
            alarmChannelMessage.setAlarmSn(Integer.parseInt(deviceAlarm.getAlarmMethod()));
            alarmChannelMessage.setAlarmDescription(deviceAlarm.getAlarmDescription());
            alarmChannelMessage.setGbId(channelId);
            redisCatchStorage.sendAlarmMsg(alarmChannelMessage);

            return;
        }

        logger.debug("存储报警信息、报警分类");
        // 存储报警信息、报警分类
        if (sipConfig.isAlarm()) {
            deviceAlarmService.add(deviceAlarm);
        }


        if (redisCatchStorage.deviceIsOnline(device.getDeviceId())) {
            publisher.deviceAlarmEventPublish(deviceAlarm);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {
        logger.info("收到来自平台[{}]的报警通知", parentPlatform.getServerGBId());
        // 回复200 OK
        try {
            responseAck(evt, Response.OK);
        } catch (SipException e) {
            throw new RuntimeException(e);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
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

        if (!StringUtils.isEmpty(deviceAlarm.getAlarmMethod())) {

            if (deviceAlarm.getAlarmMethod().contains(DeviceAlarmMethod.Video.getVal() + "")) {
                deviceAlarm.setAlarmType(getText(rootElement.element("Info"), "AlarmType"));
            }
        }

        if (channelId.equals(parentPlatform.getDeviceGBId())) {
            // 发送给平台的报警信息。 发送redis通知
            AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
            alarmChannelMessage.setAlarmSn(Integer.parseInt(deviceAlarm.getAlarmMethod()));
            alarmChannelMessage.setAlarmDescription(deviceAlarm.getAlarmDescription());
            alarmChannelMessage.setGbId(channelId);
            redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
            return;
        }
    }
}
