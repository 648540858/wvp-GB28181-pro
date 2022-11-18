package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收redis返回的推流结果
 * @author lin
 */
@Component
public class RedisPushStreamResponseListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisPushStreamResponseListener.class);

    private boolean taskQueueHandlerRun = false;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    private Map<String, PushStreamResponseEvent> responseEvents = new ConcurrentHashMap<>();

    public interface PushStreamResponseEvent{
        void run(MessageForPushChannelResponse response);
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.warn("[REDIS消息-请求推流结果]： {}", new String(message.getBody()));
        taskQueue.offer(message);
        if (!taskQueueHandlerRun) {
            taskQueueHandlerRun = true;
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    MessageForPushChannelResponse response = JSON.parseObject(new String(msg.getBody()), MessageForPushChannelResponse.class);
                    if (response == null || ObjectUtils.isEmpty(response.getApp()) || ObjectUtils.isEmpty(response.getStream())){
                        logger.info("[REDIS消息-请求推流结果]：参数不全");
                        continue;
                    }
                    // 查看正在等待的invite消息
                    if (responseEvents.get(response.getApp() + response.getStream()) != null) {
                        responseEvents.get(response.getApp() + response.getStream()).run(response);
                    }
                }
                taskQueueHandlerRun = false;
            });
        }
    }

    public void addEvent(String app, String stream, PushStreamResponseEvent callback) {
        responseEvents.put(app + stream, callback);
    }

    public void removeEvent(String app, String stream) {
        responseEvents.remove(app + stream);
    }
}
