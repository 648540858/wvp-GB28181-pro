package com.genersoft.iot.vmp.gat1400.backend.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.listener.event.WebSocketCloseEvent;

@Component
@ServerEndpoint("/VIID/Subscribe/WebSocket")
public class WebSocketEndpoint {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEndpoint.class);

    /**
     * concurrent包的线程安全Set, 用来存放每个客户端对应的MyWebSocket对象.
     */
    private static final CopyOnWriteArraySet<WebSocketEndpoint> webSocketSet = new CopyOnWriteArraySet<>();
    /**
     * 与某个客户端的连接会话, 需要通过它来给客户端发送数据.
     */
    private Session session;

    private VIIDServer loginUser;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        //webSocket中继过来的security认证状态
        this.loginUser = ((NodeDevice) ((Authentication) session.getUserPrincipal()).getPrincipal()).originVIIDServer();
        this.session = session;
        webSocketSet.add(this);
        log.info("有新连接加入! 客户端组ID为: " + this.loginUser.getServerId());
        try {
            sendMessage("连接成功xxx");
        } catch (IOException e) {
            log.error("web-socket IO异常", e);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        SpringContextHolder.publishEvent(new WebSocketCloseEvent(loginUser));
        // 从set中删除
        webSocketSet.remove(this);
        log.info("有一连接关闭！");
    }

    /**
     * 收到客户端消息后转发消息
     *
     * @param message 客户端发送过来的消息, 格式要求为: {"msg":"测试","groupId":1}
     * @param session 客户端session
     */
    @OnMessage(maxMessageSize = 999999L)
    public void onMessage(String message, Session session) throws IOException {
        log.info("来自客户端(sessionId为: " + session.getId() + ")的消息: " + message);
    }

    /**
     * web-socket发生错误
     *
     * @param session 某个session
     * @param error   错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("sessionId为:{}视图库ID:{}的session发生错误:{}", session.getId(), loginUser.getServerId(), error.getMessage());
    }

    /**
     * 向客户端发送消息
     *
     * @param message 消息
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public String getServerId() {
        return this.loginUser.getServerId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebSocketEndpoint webSocket = (WebSocketEndpoint) o;
        return Objects.equals(session, webSocket.session) &&
                Objects.equals(getServerId(), webSocket.getServerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, getServerId());
    }

    public static void sendMessageToServerId(String serverId, String message) throws IOException {
        Objects.requireNonNull(serverId, "缺少发送目标");
        for (WebSocketEndpoint webSocketEndpoint : webSocketSet) {
            if (webSocketEndpoint.getServerId().startsWith(serverId)) {
                webSocketEndpoint.sendMessage(message);
            }
        }
    }
}
