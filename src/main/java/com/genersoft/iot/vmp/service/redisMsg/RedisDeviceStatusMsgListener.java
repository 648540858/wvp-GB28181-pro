package com.genersoft.iot.vmp.service.redisMsg;


import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 监听 redis 设备状态信息
 * @author davidche
 */
@Component
public class RedisDeviceStatusMsgListener implements MessageListener {


    private final static Logger logger = LoggerFactory.getLogger(RedisGpsMsgListener.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private UserSetting userSetting;

    /**
     * 处理 接收的 redis 设备状态信息
     *
     * @param message message must not be {@literal null}.
     * @param pattern pattern matching the channel (if specified) - can be {@literal null}.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {

        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {

                        DeviceStatusMsg deviceStatusMsg = praseMessage(new String(msg.getBody()));

//                        Device device = redisCatchStorage.getDevice(deviceStatusMsg.getDeviceId());
                        Device device = storager.queryVideoDevice(deviceStatusMsg.getDeviceId());
                        DeviceChannel channel = new DeviceChannel();
                        channel.setDeviceId(deviceStatusMsg.getDeviceId());
                        channel.setChannelId(deviceStatusMsg.getChannelId());
                        switch (deviceStatusMsg.getCmd()) {
                            case CatalogEvent.ON:
                                // 上线
                                logger.info("[收到REDIS的DEVICE通道上线通知] 来自设备: {}, 通道 {}", deviceStatusMsg.getDeviceId(), deviceStatusMsg.getChannelId());
                                storager.deviceChannelOnline(deviceStatusMsg.getDeviceId(), deviceStatusMsg.getChannelId());
                                break;

                            case CatalogEvent.OFF:
                                // 离线
                                logger.info("[收到REDIS的DEVICE通道离线通知] 来自设备: {}, 通道 {}", deviceStatusMsg.getDeviceId(), deviceStatusMsg.getChannelId());
                                if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
                                    storager.deviceChannelOffline(deviceStatusMsg.getDeviceId(), deviceStatusMsg.getChannelId());
                                } else {
                                    logger.info("[收到REDIS的DEVICE通道离线通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", deviceStatusMsg.getDeviceId(), deviceStatusMsg.getChannelId());
                                }
                                break;
                            case CatalogEvent.ADD:
                                // 增加
                                logger.info("[收到REDIS的DEVICE增加通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), deviceStatusMsg.getChannelId());
                                deviceChannelService.updateChannel(deviceStatusMsg.getDeviceId(), channel);
                                deviceService.sync(device);
                                break;
                            case "DELETE":

                                // 删除
                                logger.info("[收到REDIS的DEVICE删除通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), deviceStatusMsg.getChannelId());
                                storager.delChannel(deviceStatusMsg.getDeviceId(), deviceStatusMsg.getChannelId());
                                break;

                            case CatalogEvent.UPDATE:
                                // 更新
                                logger.info("[收到REDIS的DEVICE更新通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), deviceStatusMsg.getChannelId());
                                deviceChannelService.updateChannel(deviceStatusMsg.getDeviceId(), channel);
                                deviceService.sync(device);
                                break;						case CatalogEvent.VLOST:
                                // 视频丢失
                                logger.info("[收到REDIS的DEVICE通道视频丢失通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
                                if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
                                    storager.deviceChannelOffline(deviceStatusMsg.getDeviceId(), channel.getChannelId());
                                }else {
                                    logger.info("[收到REDIS的DEVICE通道视频丢失通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
                                }
                                break;
                            case CatalogEvent.DEFECT:
                                // 故障
                                logger.info("[收到REDIS的DEVICE通道视频故障通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
                                break;

                            default:
                                logger.warn("[ NotifyCatalog ] event not found ： {}", msg.getBody());
                        }

                    } catch (Exception e) {
                        logger.warn("[REDIS的DEVICE通知] 发现未处理的异常, \r\n{}", msg.getBody());
                        logger.error("[REDIS的DEVICE通知] 异常内容： ", e);
                    }
                }
            });
        }
    }

    /**
     * 解析redis 消息
     *
     * @param message
     * @return
     */
    private DeviceStatusMsg praseMessage(String message) {
        DeviceStatusMsg deviceStatusMsg = new DeviceStatusMsg();

        if(message.split(" ")[0].split(":").length>1){
            deviceStatusMsg.setChannelId(message.split(" ")[0].split(":")[1]);
        }
        deviceStatusMsg.setDeviceId(message.split(" ")[0].split(":")[0]);
        deviceStatusMsg.setCmd(message.split(" ")[1]);

        return deviceStatusMsg;

    }

    /**
     * redis 消息对象
     */
    private class DeviceStatusMsg {
        private String deviceId;
        private String channelId;
        private String cmd;

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getChannelId() {
            return channelId;
        }

        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }

        public String getCmd() {
            return cmd;
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }
    }


}
