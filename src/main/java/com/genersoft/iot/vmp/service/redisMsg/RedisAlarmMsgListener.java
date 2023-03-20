package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class RedisAlarmMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisAlarmMsgListener.class);

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IVideoManagerStorage storage;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        // 消息示例：  PUBLISH alarm_receive '{ "gbId": "", "alarmSn": 1, "alarmType": "111", "alarmDescription": "222", }'
        logger.info("收到来自REDIS的ALARM通知： {}", new String(message.getBody()));
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
//            logger.info("[线程池信息]活动线程数：{}, 最大线程数： {}", taskExecutor.getActiveCount(), taskExecutor.getMaxPoolSize());
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        AlarmChannelMessage alarmChannelMessage = JSON.parseObject(msg.getBody(), AlarmChannelMessage.class);
                        if (alarmChannelMessage == null) {
                            logger.warn("[REDIS的ALARM通知]消息解析失败");
                            continue;
                        }
                        String gbId = alarmChannelMessage.getGbId();

                        DeviceAlarm deviceAlarm = new DeviceAlarm();
                        deviceAlarm.setCreateTime(DateUtil.getNow());
                        deviceAlarm.setChannelId(gbId);
                        deviceAlarm.setAlarmDescription(alarmChannelMessage.getAlarmDescription());
                        deviceAlarm.setAlarmMethod("" + alarmChannelMessage.getAlarmSn());
                        deviceAlarm.setAlarmType("" + alarmChannelMessage.getAlarmType());
                        deviceAlarm.setAlarmPriority("1");
                        deviceAlarm.setAlarmTime(DateUtil.getNow());
                        deviceAlarm.setLongitude(0);
                        deviceAlarm.setLatitude(0);

                        if (ObjectUtils.isEmpty(gbId)) {
                            if (userSetting.getSendToPlatformsWhenIdLost()) {
                                // 发送给所有的上级
                                List<ParentPlatform> parentPlatforms = storage.queryEnableParentPlatformList(true);
                                if (parentPlatforms.size() > 0) {
                                    for (ParentPlatform parentPlatform : parentPlatforms) {
                                        try {
                                            deviceAlarm.setChannelId(parentPlatform.getDeviceGBId());
                                            commanderForPlatform.sendAlarmMessage(parentPlatform, deviceAlarm);
                                        } catch (SipException | InvalidArgumentException | ParseException e) {
                                            logger.error("[命令发送失败] 国标级联 发送报警: {}", e.getMessage());
                                        }
                                    }
                                }
                            }else {
                                // 获取开启了消息推送的设备和平台
                                List<ParentPlatform> parentPlatforms = storage.queryEnablePlatformListWithAsMessageChannel();
                                if (parentPlatforms.size() > 0) {
                                    for (ParentPlatform parentPlatform : parentPlatforms) {
                                        try {
                                            deviceAlarm.setChannelId(parentPlatform.getDeviceGBId());
                                            commanderForPlatform.sendAlarmMessage(parentPlatform, deviceAlarm);
                                        } catch (SipException | InvalidArgumentException | ParseException e) {
                                            logger.error("[命令发送失败] 国标级联 发送报警: {}", e.getMessage());
                                        }
                                    }
                                }

                            }
                            // 获取开启了消息推送的设备和平台
                            List<Device> devices = storage.queryDeviceWithAsMessageChannel();
                            if (devices.size() > 0) {
                                for (Device device : devices) {
                                    try {
                                        deviceAlarm.setChannelId(device.getDeviceId());
                                        commander.sendAlarmMessage(device, deviceAlarm);
                                    } catch (InvalidArgumentException | SipException | ParseException e) {
                                        logger.error("[命令发送失败] 发送报警: {}", e.getMessage());
                                    }
                                }
                            }

                        }else {
                            Device device = storage.queryVideoDevice(gbId);
                            ParentPlatform platform = storage.queryParentPlatByServerGBId(gbId);
                            if (device != null && platform == null) {
                                try {
                                    commander.sendAlarmMessage(device, deviceAlarm);
                                } catch (InvalidArgumentException | SipException | ParseException e) {
                                    logger.error("[命令发送失败] 发送报警: {}", e.getMessage());
                                }
                            }else if (device == null && platform != null){
                                try {
                                    commanderForPlatform.sendAlarmMessage(platform, deviceAlarm);
                                } catch (InvalidArgumentException | SipException | ParseException e) {
                                    logger.error("[命令发送失败] 发送报警: {}", e.getMessage());
                                }
                            }else {
                                logger.warn("无法确定" + gbId + "是平台还是设备");
                            }
                        }
                    }catch (Exception e) {
                        logger.error("未处理的异常 ", e);
                        logger.warn("[REDIS的ALARM通知] 发现未处理的异常, {}",e.getMessage());
                    }
                }
            });
        }
    }
}
