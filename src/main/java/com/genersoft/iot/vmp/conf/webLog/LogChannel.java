package com.genersoft.iot.vmp.conf.webLog;

import lombok.extern.slf4j.Slf4j;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerEndpoint(value = "/channel/log")
@Slf4j
public class LogChannel {

    public static final ConcurrentMap<String, LogChannel> CHANNELS = new ConcurrentHashMap<>();

    private Session session;

    @OnMessage(maxMessageSize = 1) // MaxMessage 1 byte
    public void onMessage(String message) {

        try {
            this.session.close(new CloseReason(CloseReason.CloseCodes.TOO_BIG, "此节点不接收任何客户端信息"));
        } catch (IOException e) {
            log.error("[Web-Log] 连接关闭失败: id={}, err={}", this.session.getId(), e.getMessage());
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        this.session.setMaxIdleTimeout(0);
        CHANNELS.put(this.session.getId(), this);

        log.info("[Web-Log] 连接已建立: id={}", this.session.getId());
    }

    @OnClose
    public void onClose(CloseReason closeReason) {

        log.info("[Web-Log] 连接已断开: id={}, err={}", this.session.getId(), closeReason);
        CHANNELS.remove(this.session.getId());
    }

    @OnError
    public void onError(Throwable throwable) throws IOException {
        log.info("[Web-Log] 连接错误: id={}, err= {}", this.session.getId(), throwable.getMessage());
        if (this.session.isOpen()) {
            this.session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
        }
    }

    /**
     * Push messages to all clients
     *
     * @param message
     */
    public static void push(String message) {
        CHANNELS.values().stream().forEach(endpoint -> {
            if (endpoint.session.isOpen()) {
                endpoint.session.getAsyncRemote().sendText(message);
            }
        });
    }
}
