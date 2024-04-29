package com.genersoft.iot.vmp.jt1078.session;

import com.genersoft.iot.vmp.jt1078.proc.entity.Cmd;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;


/**
 * @author QingtaiJiang
 * @date 2023/4/27 19:54
 * @email qingtaij@163.com
 */
public enum SessionManager {
    INSTANCE;
    private final static Logger log = LoggerFactory.getLogger(SessionManager.class);

    // 用与消息的缓存
    private final Map<String, SynchronousQueue<Object>> topicSubscribers = new ConcurrentHashMap<>();

    // session的缓存
    private final Map<Object, Session> sessionMap;

    SessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
    }

    /**
     * 创建新的Session
     *
     * @param channel netty通道
     * @return 创建的session对象
     */
    public Session newSession(Channel channel) {
        return new Session(channel);
    }


    /**
     * 获取指定设备的Session
     *
     * @param clientId 设备Id
     * @return Session
     */
    public Session get(Object clientId) {
        return sessionMap.get(clientId);
    }

    /**
     * 放入新设备连接的session
     *
     * @param clientId   设备ID
     * @param newSession session
     */
    protected void put(Object clientId, Session newSession) {
        sessionMap.put(clientId, newSession);
    }


    /**
     * 发送同步消息，接收响应
     * 默认超时时间6秒
     */
    public Object request(Cmd cmd) {
        // 默认6秒
        int timeOut = 6000;
        return request(cmd, timeOut);
    }

    public Object request(Cmd cmd, Integer timeOut) {
        Session session = this.get(cmd.getDevId());
        if (session == null) {
            log.error("DevId: {} not online!", cmd.getDevId());
            return null;
        }
        String requestKey = requestKey(cmd.getDevId(), cmd.getRespId(), cmd.getPackageNo());
        SynchronousQueue<Object> subscribe = subscribe(requestKey);
        if (subscribe == null) {
            log.error("DevId: {} key:{} send repaid", cmd.getDevId(), requestKey);
            return null;
        }
        session.writeObject(cmd);
        try {
            return subscribe.poll(timeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("<<<<<<<<<< timeout" + session, e);
        } finally {
            this.unsubscribe(requestKey);
        }
        return null;
    }

    public Boolean response(String devId, String respId, Long responseNo, Object data) {
        String requestKey = requestKey(devId, respId, responseNo);
        SynchronousQueue<Object> queue = topicSubscribers.get(requestKey);
        if (queue != null) {
            try {
                return queue.offer(data, 2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
        log.warn("Not find response,key:{} data:{} ", requestKey, data);
        return false;
    }

    public Boolean response(String key, Object data) {
        SynchronousQueue<Object> queue = topicSubscribers.get(key);
        if (queue != null) {
            try {
                return queue.offer(data, 2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
        log.warn("Not find response,key:{} data:{} ", key, data);
        return false;
    }

    public List<String> getAllRequestKey() {
        return new ArrayList<>(topicSubscribers.keySet());
    }

    private void unsubscribe(String key) {
        topicSubscribers.remove(key);
    }

    private SynchronousQueue<Object> subscribe(String key) {
        SynchronousQueue<Object> queue = null;
        if (!topicSubscribers.containsKey(key))
            topicSubscribers.put(key, queue = new SynchronousQueue<>());
        return queue;
    }

    private String requestKey(String devId, String respId, Long requestNo) {
        return String.join("_", devId.replaceFirst("^0*", ""), respId, requestNo.toString());
    }

    public void remove(String devId) {
        sessionMap.remove(devId);
    }
}
