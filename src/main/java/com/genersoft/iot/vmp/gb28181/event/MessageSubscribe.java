package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.utils.DelayQueueConsumer;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

/**
 * @author lin
 */
@Slf4j
@Component
public class MessageSubscribe {

    private final Map<String, MessageEvent<?>> subscribes = new ConcurrentHashMap<>();

    private final DelayQueue<MessageEvent<?>> delayQueue = new DelayQueue<>();

    private Thread consumerThread;

    @PostConstruct
    public void start() {
        consumerThread = DelayQueueConsumer.start("message-subscribe-consumer", delayQueue, event -> {
            try {
                if (event.getCallback() != null) {
                    event.getCallback().run(ErrorCode.ERROR486.getCode(), "消息超时未回复", null);
                }
            } finally {
                subscribes.remove(event.getKey());
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    public void addSubscribe(MessageEvent<?> event) {
        MessageEvent<?> messageEvent = subscribes.get(event.getKey());
        if (messageEvent != null) {
            subscribes.remove(event.getKey());
            delayQueue.remove(messageEvent);
        }
        subscribes.put(event.getKey(), event);
        delayQueue.offer(event);
    }

    public MessageEvent<?> getSubscribe(String key) {
        return subscribes.get(key);
    }

    public void removeSubscribe(String key) {
        if(key == null){
            return;
        }
        MessageEvent<?> messageEvent = subscribes.get(key);
        if (messageEvent != null) {
            subscribes.remove(key);
            delayQueue.remove(messageEvent);
        }
    }

    public boolean isEmpty(){
        return subscribes.isEmpty();
    }

    public Integer size() {
        return subscribes.size();
    }
}
