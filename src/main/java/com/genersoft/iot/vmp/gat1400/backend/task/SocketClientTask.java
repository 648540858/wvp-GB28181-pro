package com.genersoft.iot.vmp.gat1400.backend.task;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.backend.socket.client.SocketDataConsumer;
import com.genersoft.iot.vmp.gat1400.backend.socket.client.WebsocketClient;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDServerService;
import com.genersoft.iot.vmp.gat1400.listener.event.WebSocketCloseEvent;

import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocketClientTask {
    private final Map<String, SocketClient> socketSubscribeMap = new ConcurrentHashMap<>();
    private final String pingMessage = new JSONObject().fluentPut("type", "ping").toJSONString();
    @Resource
    VIIDServerService service;

    @Scheduled(initialDelay = 20, fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void socketClientIdle() {
        Set<String> keySet = socketSubscribeMap.keySet();
        for (String key : keySet) {
            SocketClient client = socketSubscribeMap.get(key);
            if (Objects.nonNull(client)) {
                if (client.future.isDone()) {
                    socketSubscribeMap.remove(key);
                } else {
                    client.client.send(this.pingMessage);
                }
            }
        }
    }

    @Order(1)
    @PreDestroy
    public void destroy() {
        socketSubscribeMap.forEach((key, value) -> value.client.close());
    }

    public synchronized void serverSocketAdd(VIIDServer server) {
        String serverId = server.getServerId();
        log.info("启动视图库{}WebSocket连接...", serverId);
        SocketClient socketClient = socketSubscribeMap.get(serverId);
        if (Objects.isNull(socketClient) || socketClient.future.isDone()) {
            this.serverSocketRemove(serverId);
            SocketClient client = new SocketClient();
            VIIDServer setting = service.getCurrentServer();
            String url = String.format("ws://%s:%s/VIID/Subscribe/WebSocket", server.getHost(), server.getPort());
            client.client = new WebsocketClient(url, setting.getServerId(), SocketDataConsumer.from(serverId));
            client.future = CompletableFuture.runAsync(client.client).whenCompleteAsync((unused, throwable) -> {
                if (Objects.nonNull(throwable)) {
                    log.info("WebSocket异常中断,发布关闭事件{}", throwable.getClass());
                    SpringContextHolder.publishEvent(new WebSocketCloseEvent(server));
                }
            });
            socketSubscribeMap.put(serverId, client);
            log.info("启动视图库{}的WebSocket连接完成", server.getServerId());
        }
    }

    public synchronized void serverSocketRemove(String serverId) {
        SocketClient client = socketSubscribeMap.remove(serverId);
        if (Objects.nonNull(client) && !client.future.isDone()) {
            client.client.close();
            log.info("断开视图库{}的WebSocket连接", serverId);
        }
    }

    public static class SocketClient {
        private WebsocketClient client;
        private CompletableFuture<Void> future;

    }
}
