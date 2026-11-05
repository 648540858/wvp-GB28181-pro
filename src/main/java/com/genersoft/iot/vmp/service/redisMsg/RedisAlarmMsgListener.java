package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.AlarmChannelMessage;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import com.genersoft.iot.vmp.utils.DateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 监听 SUBSCRIBE alarm_receive
 * 发布 PUBLISH alarm_receive '{ "gbId": "", "alarmSn": 1, "alarmType": "111", "alarmDescription": "222", }'
 */
@Slf4j
@Component
public class RedisAlarmMsgListener implements MessageListener {

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private IMobilePositionService mobilePositionService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    private final BlockingQueue<Message> taskQueue = new LinkedBlockingQueue<>();

    @Autowired
    private UserSetting userSetting;

    private Thread consumerThread;

    @PostConstruct
    public void start() {
        consumerThread = new Thread(this::consume, "redis-alarm-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @PreDestroy
    public void stop() {
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processMessage(taskQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("[REDIS的ALARM通知] 消费异常", e);
            }
        }
    }

    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        log.info("[REDIS: ALARM]： {}", new String(message.getBody()));
        taskQueue.offer(message);
    }

    private void processMessage(Message msg) {
        try {
            AlarmChannelMessage alarmChannelMessage = JSON.parseObject(msg.getBody(), AlarmChannelMessage.class);
            if (alarmChannelMessage == null) {
                log.warn("[REDIS的ALARM通知]消息解析失败");
                return;
            }
            String chanelId = alarmChannelMessage.getGbId();

            DeviceAlarmNotify deviceAlarm = new DeviceAlarmNotify();
            deviceAlarm.setCreateTime(DateUtil.getNow());
            deviceAlarm.setChannelId(chanelId);
            deviceAlarm.setAlarmDescription(alarmChannelMessage.getAlarmDescription());
            deviceAlarm.setAlarmMethod(alarmChannelMessage.getAlarmSn());
            deviceAlarm.setAlarmType(alarmChannelMessage.getAlarmType());
            deviceAlarm.setAlarmPriority("1");
            deviceAlarm.setAlarmTime(DateUtil.getNow());
            deviceAlarm.setLongitude(0);
            deviceAlarm.setLatitude(0);

            if (ObjectUtils.isEmpty(chanelId)) {
                if (userSetting.getSendToPlatformsWhenIdLost()) {
                    // 发送给所有的上级
                    List<Platform> parentPlatforms = platformService.queryEnablePlatformList(userSetting.getServerId());
                    if (!parentPlatforms.isEmpty()) {
                        for (Platform parentPlatform : parentPlatforms) {
                            try {
                                deviceAlarm.setChannelId(parentPlatform.getDeviceGBId());
                                commanderForPlatform.sendAlarmMessage(parentPlatform, deviceAlarm);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 国标级联 发送报警: {}", e.getMessage());
                            }
                        }
                    }
                } else {
                    // 获取开启了消息推送的设备和平台
                    List<Platform> parentPlatforms = mobilePositionService.queryEnablePlatformListWithAsMessageChannel();
                    if (!parentPlatforms.isEmpty()) {
                        for (Platform parentPlatform : parentPlatforms) {
                            try {
                                deviceAlarm.setChannelId(parentPlatform.getDeviceGBId());
                                commanderForPlatform.sendAlarmMessage(parentPlatform, deviceAlarm);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 国标级联 发送报警: {}", e.getMessage());
                            }
                        }
                    }
                }
                // 获取开启了消息推送的设备和平台
                List<Device> devices = channelService.queryDeviceWithAsMessageChannel();
                if (!devices.isEmpty()) {
                    for (Device device : devices) {
                        try {
                            deviceAlarm.setChannelId(device.getDeviceId());
                            commander.sendAlarmMessage(device, deviceAlarm);
                        } catch (InvalidArgumentException | SipException | ParseException e) {
                            log.error("[命令发送失败] 发送报警: {}", e.getMessage());
                        }
                    }
                }
            } else {
                // 获取该通道ID是属于设备还是对应的上级平台
                Device device = deviceService.getDeviceBySourceChannelDeviceId(chanelId);
                List<Platform> platforms = platformChannelService.queryByPlatformBySharChannelId(chanelId);
                if (device != null && device.getServerId().equals(userSetting.getServerId()) && (platforms == null || platforms.isEmpty())) {
                    try {
                        commander.sendAlarmMessage(device, deviceAlarm);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[命令发送失败] 发送报警: {}", e.getMessage());
                    }
                } else if (device == null && (platforms != null && !platforms.isEmpty() )) {
                    for (Platform platform : platforms) {
                        if (platform.getServerId().equals(userSetting.getServerId())) {
                            try {
                                commanderForPlatform.sendAlarmMessage(platform, deviceAlarm);
                            } catch (InvalidArgumentException | SipException | ParseException e) {
                                log.error("[命令发送失败] 发送报警: {}", e.getMessage());
                            }
                        }
                    }
                } else {
                    log.warn("[REDIS的ALARM通知] 未查询到" + chanelId + "所属的平台或设备");
                }
            }
        } catch (Exception e) {
            log.error("未处理的异常 ", e);
            log.warn("[REDIS的ALARM通知] 发现未处理的异常, {}", e.getMessage());
        }
    }
}
