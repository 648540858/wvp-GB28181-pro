package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.service.bean.PushStreamStatusChangeFromRedisDto;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * 接收redis发送的推流设备上线下线通知
 *
 * @author lin
 * 发送 PUBLISH VM_MSG_PUSH_STREAM_STATUS_CHANGE '{"setAllOffline":false,"offlineStreams":[{"app":"1000","stream":"10000022","timeStamp":1726729716551}]}'
 * 订阅 SUBSCRIBE VM_MSG_PUSH_STREAM_STATUS_CHANGE
 */
@Slf4j
@Component
public class RedisPushStreamStatusMsgListener implements MessageListener, ApplicationRunner {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("[REDIS: 流设备状态变化]： {}", new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 100)
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<Message> messageDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            Message msg = taskQueue.poll();
            if (msg != null) {
                messageDataList.add(msg);
            }
        }
        if (messageDataList.isEmpty()) {
            return;
        }
        for (Message msg : messageDataList) {
            try {
                PushStreamStatusChangeFromRedisDto streamStatusMessage = JSON.parseObject(msg.getBody(), PushStreamStatusChangeFromRedisDto.class);
                if (streamStatusMessage == null) {
                    log.warn("[REDIS消息]推流设备状态变化消息解析失败");
                    continue;
                }
                // 取消定时任务
                dynamicTask.stop(VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED);
                if (streamStatusMessage.isSetAllOffline()) {
                    // 所有设备离线
                    streamPushService.allOffline();
                }
                if (streamStatusMessage.getOfflineStreams() != null
                        && !streamStatusMessage.getOfflineStreams().isEmpty()) {
                    // 更新部分设备离线
                    streamPushService.offline(streamStatusMessage.getOfflineStreams());
                }
                if (streamStatusMessage.getOnlineStreams() != null &&
                        !streamStatusMessage.getOnlineStreams().isEmpty()) {
                    // 更新部分设备上线
                    streamPushService.online(streamStatusMessage.getOnlineStreams());
                }
            } catch (Exception e) {
                log.warn("[REDIS消息-推流设备状态变化] 发现未处理的异常, \r\n{}", JSON.parseObject(msg.getBody()));
                log.error("[REDIS消息-推流设备状态变化] 异常内容： ", e);
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userSetting.getUsePushingAsStatus()) {
            return;
        }
        // 查询是否存在推流设备,没有则不发送
        List<String> allAppAndStream = streamPushService.getAllAppAndStream();
        if (allAppAndStream == null || allAppAndStream.isEmpty()) {
            return;
        }
        //  启动时设置所有推流通道离线，发起查询请求
        redisCatchStorage.sendStreamPushRequestedMsgForStatus();
        dynamicTask.startDelay(VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED, () -> {
            log.info("[REDIS消息]未收到redis回复推流设备状态，执行推流设备离线");
            // 五秒收不到请求就设置通道离线，然后通知上级离线
            streamPushService.allOffline();
        }, 5000);
    }

}
