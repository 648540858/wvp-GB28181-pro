package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.CommonCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用回调管理
 */
@Component
public class CommonSessionManager {

    public static Map<String, CommonSession> callbackMap = new ConcurrentHashMap<>();

    /**
     * 存储回调相关的信息
     */
    class CommonSession{
        public String session;
        public long createTime;
        public int timeout;

        public CommonCallback<Object> callback;
        public CommonCallback<String> timeoutCallback;
    }

    /**
     * 添加回调
     * @param sessionId 唯一标识
     * @param callback 回调
     * @param timeout 超时时间, 单位分钟
     */
    public void add(String sessionId, CommonCallback<Object> callback, CommonCallback<String> timeoutCallback,
                    Integer timeout) {
        CommonSession commonSession = new CommonSession();
        commonSession.session = sessionId;
        commonSession.callback = callback;
        commonSession.createTime = System.currentTimeMillis();
        if (timeoutCallback != null) {
            commonSession.timeoutCallback = timeoutCallback;
        }
        if (timeout != null) {
            commonSession.timeout = timeout;
        }
        callbackMap.put(sessionId, commonSession);
    }

    public void add(String sessionId, CommonCallback<Object> callback) {
        add(sessionId, callback, null, 1);
    }

    public CommonCallback<Object> get(String sessionId, boolean destroy) {
        CommonSession commonSession = callbackMap.get(sessionId);
        if (destroy) {
            callbackMap.remove(sessionId);
        }
        return commonSession.callback;
    }

    public CommonCallback<Object> get(String sessionId) {
        return get(sessionId, false);
    }

    public void delete(String sessionID) {
        callbackMap.remove(sessionID);
    }

    @Scheduled(fixedRate= 60)   //每分钟执行一次
    public void execute(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -1);
        for (String session : callbackMap.keySet()) {
            if (callbackMap.get(session).createTime < cal.getTimeInMillis()) {
                // 超时
                if (callbackMap.get(session).timeoutCallback != null) {
                    callbackMap.get(session).timeoutCallback.run("timeout");
                }
                callbackMap.remove(session);
            }
        }
    }
}
