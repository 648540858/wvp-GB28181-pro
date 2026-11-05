package com.genersoft.iot.vmp.conf.trace;

import com.alibaba.fastjson2.JSON;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 国标级联链路追踪 WebSocket 推送通道。
 * <p>
 * 页面通过 {@code /channel/trace} 连接，只读，不接收客户端消息。
 * 每个连接使用独立队列和单线程顺序发送，避免 getAsyncRemote() 的
 * TEXT_FULL_WRITING 导致历史事件或实时事件丢失。
 */
@ServerEndpoint(value = "/channel/trace")
@Slf4j
public class TraceChannel {

    private static final int MAX_RECENT = 500;

    private static final int MAX_QUEUE = 2000;

    private static final ConcurrentMap<String, TraceChannel> CHANNELS = new ConcurrentHashMap<>();

    /**
     * 最近事件缓存，页面刷新/重连后可以恢复
     */
    private static final Deque<String> RECENT = new ConcurrentLinkedDeque<>();

    private Session session;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(MAX_QUEUE);

    private volatile boolean running = true;

    private Thread senderThread;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.session.setMaxIdleTimeout(0);
        CHANNELS.put(session.getId(), this);

        // 历史事件先入队；实时事件也可能在这期间入队，页面会按 eventId 去重。
        for (String event : RECENT) {
            enqueue(event);
        }

        senderThread = new Thread(this::sendLoop, "trace-sender-" + session.getId());
        senderThread.setDaemon(true);
        senderThread.start();

        log.info("[Trace] 连接已建立: id={}, 当前连接数={}", session.getId(), CHANNELS.size());
    }

    @OnClose
    public void onClose(CloseReason reason) {
        String id = sessionId();
        stop();
        CHANNELS.remove(id);
        log.info("[Trace] 连接已断开: id={}, reason={}", id, reason);
    }

    @OnError
    public void onError(Throwable throwable) {
        String id = sessionId();
        stop();
        CHANNELS.remove(id);
        log.info("[Trace] 连接错误: id={}, err={}", id, throwable.getMessage());
    }

    @OnMessage
    public void onMessage(String message) {
        // 只读通道，忽略客户端消息
    }

    public static void push(TraceEvent event) {
        if (event == null) {
            return;
        }
        String json = JSON.toJSONString(event);
        RECENT.addLast(json);
        while (RECENT.size() > MAX_RECENT) {
            RECENT.pollFirst();
        }
        CHANNELS.values().forEach(channel -> channel.send(json));
    }

    private void send(String json) {
        if (!running || session == null || !session.isOpen()) {
            return;
        }
        enqueue(json);
    }

    private void enqueue(String json) {
        if (json == null) {
            return;
        }
        if (!queue.offer(json)) {
            // 页面消费过慢时丢弃最旧的事件，保证内存不无限增长
            queue.poll();
            queue.offer(json);
        }
    }

    private void sendLoop() {
        while (running && session != null && session.isOpen()) {
            try {
                String json = queue.poll(1, TimeUnit.SECONDS);
                if (json == null) {
                    continue;
                }
                // 阻塞式串行发送，同一连接永远只有一个写操作
                session.getBasicRemote().sendText(json);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.debug("[Trace] 发送事件失败: id={}, err={}", sessionId(), e.getMessage());
                closeSession();
                break;
            }
        }
        stop();
    }

    private void stop() {
        running = false;
        queue.clear();
        if (senderThread != null) {
            senderThread.interrupt();
        }
    }

    private void closeSession() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String sessionId() {
        return session != null ? session.getId() : "-";
    }
}
