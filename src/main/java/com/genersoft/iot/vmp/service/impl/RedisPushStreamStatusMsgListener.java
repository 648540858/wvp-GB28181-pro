package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.PushStreamStatusChangeFromRedisDto;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * 接收redis发送的推流设备上线下线通知
 * @author lin
 */
@Component
public class RedisPushStreamStatusMsgListener implements MessageListener, ApplicationRunner {

    private final static Logger logger = LoggerFactory.getLogger(RedisPushStreamStatusMsgListener.class);

    private boolean taskQueueHandlerRun = false;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private DynamicTask dynamicTask;



    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        // TODO 增加队列
        logger.warn("[REDIS消息-推流设备状态变化]： {}", new String(message.getBody()));
        taskQueue.offer(message);

        if (!taskQueueHandlerRun) {
            taskQueueHandlerRun = true;
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    PushStreamStatusChangeFromRedisDto statusChangeFromPushStream = JSON.parseObject(msg.getBody(), PushStreamStatusChangeFromRedisDto.class);
                    if (statusChangeFromPushStream == null) {
                        logger.warn("[REDIS消息]推流设备状态变化消息解析失败");
                        return;
                    }
                    // 取消定时任务
                    dynamicTask.stop(VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED);
                    if (statusChangeFromPushStream.isSetAllOffline()) {
                        // 所有设备离线
                        streamPushService.allStreamOffline();
                    }
                    if (statusChangeFromPushStream.getOfflineStreams() != null
                            && statusChangeFromPushStream.getOfflineStreams().size() > 0) {
                        // 更新部分设备离线
                        streamPushService.offline(statusChangeFromPushStream.getOfflineStreams());
                    }
                    if (statusChangeFromPushStream.getOnlineStreams() != null &&
                            statusChangeFromPushStream.getOnlineStreams().size() > 0) {
                        // 更新部分设备上线
                        streamPushService.online(statusChangeFromPushStream.getOnlineStreams());
                    }
                }
                taskQueueHandlerRun = false;
            });
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //  启动时设置所有推流通道离线，发起查询请求
        redisCatchStorage.sendStreamPushRequestedMsgForStatus();
        dynamicTask.startDelay(VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED, ()->{
            logger.info("[REDIS消息]未收到redis回复推流设备状态，执行推流设备离线");
            // 五秒收不到请求就设置通道离线，然后通知上级离线
            streamPushService.allStreamOffline();
        }, 5000);
    }

}
