package com.genersoft.iot.vmp.conf.local;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 本地消息总线：内存模式下模拟 redis pub/sub，把消息同步投递给订阅的监听器。
 * 监听器保持 spring MessageListener 接口不变，仅需注册到对应频道。
 */
@Slf4j
public class LocalMsgBus {

    private final Map<String, List<MessageListener>> subscribers = new ConcurrentHashMap<>();

    public void subscribe(String channel, MessageListener listener) {
        subscribers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void convertAndSend(String channel, Object payload) {
        List<MessageListener> listeners = subscribers.get(channel);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        byte[] body = payload instanceof String string
                ? string.getBytes(StandardCharsets.UTF_8)
                : JSON.toJSONBytes(payload);
        DefaultMessage message = new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), body);
        for (MessageListener listener : listeners) {
            try {
                listener.onMessage(message, null);
            } catch (Exception e) {
                log.error("[本地消息总线] 投递消息失败，channel: {}", channel, e);
            }
        }
    }
}
