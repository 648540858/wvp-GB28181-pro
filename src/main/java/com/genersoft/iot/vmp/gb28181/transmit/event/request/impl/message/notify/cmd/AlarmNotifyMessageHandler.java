package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import okhttp3.*;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;

import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

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

    @Value("${user-settings.alarmCallbackHttp:}")
    private String alarmCallbackHttp;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        logger.info("收到来自设备[{}]的报警通知", device.getDeviceId());
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
        deviceAlarm.setDeviceId(device.getDeviceId());
        deviceAlarm.setChannelId(channelId);
        deviceAlarm.setAlarmPriority(getText(rootElement, "AlarmPriority"));
        deviceAlarm.setAlarmMethod(getText(rootElement, "AlarmMethod"));
        deviceAlarm.setAlarmTime(getText(rootElement, "AlarmTime"));
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
                mobilePosition.setDeviceId(deviceAlarm.getDeviceId());
                mobilePosition.setTime(deviceAlarm.getAlarmTime());
                mobilePosition.setLongitude(deviceAlarm.getLongitude());
                mobilePosition.setLatitude(deviceAlarm.getLatitude());
                mobilePosition.setReportSource("GPS Alarm");
                // 默认来源坐标系为WGS-84处理
                Double[] gcj02Point = Coordtransform.WGS84ToGCJ02(mobilePosition.getLongitude(), mobilePosition.getLatitude());
                logger.info("GCJ02坐标：" + gcj02Point[0] + ", " + gcj02Point[1]);
                mobilePosition.setGeodeticSystem("GCJ-02");
                mobilePosition.setCnLng(gcj02Point[0] + "");
                mobilePosition.setCnLat(gcj02Point[1] + "");
                if (!userSetting.getSavePositionHistory()) {
                    storager.clearMobilePositionsByDeviceId(device.getDeviceId());
                }
                storager.insertMobilePosition(mobilePosition);
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

        alarmCallback("device",device,null,deviceAlarm);
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
        deviceAlarm.setDeviceId(parentPlatform.getServerGBId());
        deviceAlarm.setChannelId(channelId);
        deviceAlarm.setAlarmPriority(getText(rootElement, "AlarmPriority"));
        deviceAlarm.setAlarmMethod(getText(rootElement, "AlarmMethod"));
        deviceAlarm.setAlarmTime(getText(rootElement, "AlarmTime"));
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

        alarmCallback("platform",null,parentPlatform,deviceAlarm);
    }

    /**
     * 处理报警回调外部接口处理
     * @param alarmSource 报警来源，device/platfrom
     * @param device 报警设备
     * @param parentPlatform 报警平台
     * @param deviceAlarm 报警内容
     */
    private void alarmCallback(String alarmSource, Device device, ParentPlatform parentPlatform, DeviceAlarm deviceAlarm){
        if (StringUtils.isEmpty(alarmCallbackHttp)) return;

        JSONObject alarmData = new JSONObject();
        alarmData.put("alarmSource",alarmSource);
        alarmData.put("sourcesInfo",device != null ? JSON.toJSONString(device) : JSON.toJSONString(parentPlatform));
        alarmData.put("alarmContent",JSON.toJSONString(deviceAlarm));

        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("alarmData",alarmData.toJSONString())
                    .build();
            Request request = new Request.Builder()
                    .header("Authorization", "Client-ID " + UUID.randomUUID())
                    .url(alarmCallbackHttp)
                    .post(requestBody)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) {
                        logger.debug("报警回调外部接口返回成功");
                    }else{
                        logger.debug("报警回调外部接口返回失败：{}", response.message());
                    }
                }
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.error("处理报警回调外部接口错误：{}", e.getMessage());
                }
            });
        }catch (Exception e){
            logger.error("处理报警回调外部接口错误，请检查配置文件的http地址是否正确：{}", e.getMessage());
        }
    }
}
