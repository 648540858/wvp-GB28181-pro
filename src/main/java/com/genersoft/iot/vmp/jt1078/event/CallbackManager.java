package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.common.GeneralCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理callback回调，支持设置超时时间，未设置则按照五分钟超时自动移除
 */
@Component
public class CallbackManager {

    private final long expire = 5 * 60 * 1000;

    static class ManagerCallBack {
        public String key;
        public GeneralCallback<?> callback;
        public long createTime;
        public long expire;
    }

    private final Map<String, ManagerCallBack> allCallbacks = new ConcurrentHashMap<>();


    public void addCallback(String key, GeneralCallback<?> callback) {
        ManagerCallBack managerCallBack = new ManagerCallBack();
        managerCallBack.callback = callback;
        managerCallBack.key = key;
        managerCallBack.createTime = System.currentTimeMillis();
        managerCallBack.expire = expire;
        allCallbacks.put(key, managerCallBack);
    }

    public void addCallback(String key, GeneralCallback<Object> callback, long timeout) {
        ManagerCallBack managerCallBack = new ManagerCallBack();
        managerCallBack.callback = callback;
        managerCallBack.key = key;
        managerCallBack.createTime = System.currentTimeMillis();
        managerCallBack.expire = timeout;
        allCallbacks.put(key, managerCallBack);
    }

    public GeneralCallback<?> getCallback(String key){
        ManagerCallBack managerCallBack = allCallbacks.get(key);
        if (managerCallBack != null) {
            return managerCallBack.callback;
        }else {
            return null;
        }
    }

    public void removeCallback(String key){
        allCallbacks.remove(key);
    }
    /**
     * 对订阅数据进行过期清理
     */
    @Scheduled(fixedRate=expire)   //每5分钟执行一次
    public void execute(){
        for (ManagerCallBack callBack : allCallbacks.values()) {
            if ((System.currentTimeMillis() - callBack.createTime - callBack.expire) > 0) {
                allCallbacks.remove(callBack.key);
            }
        }
    }
}
