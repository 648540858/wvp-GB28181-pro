package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收来自redis的关闭流更新通知
 * 消息举例： PUBLISH VM_MSG_STREAM_PUSH_CLOSE "{'app': 'live', 'stream': 'stream'}"
 * @author lin
 */
@Slf4j
@Component
public class RedisCloseStreamMsgListener implements MessageListener {

    @Autowired
    private IStreamPushService pushService;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        log.info("[REDIS: 关闭流]： {}", new String(message.getBody()));
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
                JSONObject jsonObject = JSON.parseObject(msg.getBody());
                String app = jsonObject.getString("app");
                String stream = jsonObject.getString("stream");
                pushService.stopByAppAndStream(app, stream);
            }catch (Exception e) {
                log.warn("[REDIS的关闭推流通知] 发现未处理的异常, \r\n{}", JSON.toJSONString(msg));
                log.error("[REDIS的关闭推流通知] 异常内容： ", e);
            }
        }
    }
}
