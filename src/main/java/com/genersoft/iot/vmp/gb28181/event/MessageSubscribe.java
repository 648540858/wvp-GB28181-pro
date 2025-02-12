package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Scheduled(fixedDelay = 200)   //每200毫秒执行
    public void execute(){
        while (!delayQueue.isEmpty()) {
            try {
                MessageEvent<?> take = delayQueue.take();
                // 出现超时异常
                if(take.getCallback() != null) {
                    take.getCallback().run(ErrorCode.ERROR486.getCode(), "消息超时未回复", null);
                }
                subscribes.remove(take.getKey());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
