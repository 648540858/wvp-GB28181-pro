package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收redis返回的推流结果
 *
 * @author lin
 * PUBLISH VM_MSG_STREAM_PUSH_RESPONSE '{"code":0,"msg":"失败","app":"1000","stream":"10000022"}'
 */
@Slf4j
@Component
public class RedisPushStreamResponseListener implements MessageListener {

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();


    private final Map<String, PushStreamResponseEvent> responseEvents = new ConcurrentHashMap<>();

    public interface PushStreamResponseEvent {
        void run(MessageForPushChannelResponse response);
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("[REDIS: 推流结果]： {}", new String(message.getBody()));
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
                MessageForPushChannelResponse response = JSON.parseObject(new String(msg.getBody()), MessageForPushChannelResponse.class);
                if (response == null || ObjectUtils.isEmpty(response.getApp()) || ObjectUtils.isEmpty(response.getStream())) {
                    log.info("[REDIS消息-请求推流结果]：参数不全");
                    continue;
                }
                // 查看正在等待的invite消息
                if (responseEvents.get(response.getApp() + response.getStream()) != null) {
                    responseEvents.get(response.getApp() + response.getStream()).run(response);
                }
            } catch (Exception e) {
                log.warn("[REDIS消息-请求推流结果] 发现未处理的异常, \r\n{}", JSON.toJSONString(msg));
                log.error("[REDIS消息-请求推流结果] 异常内容： ", e);
            }
        }
    }

    public void addEvent(String app, String stream, PushStreamResponseEvent callback) {
        responseEvents.put(app + stream, callback);
    }

    public void removeEvent(String app, String stream) {
        responseEvents.remove(app + stream);
    }
}
