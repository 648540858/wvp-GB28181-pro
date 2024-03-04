package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeHandlerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lin
 */
@Component
public class SubscribeHolder {

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    private final String taskOverduePrefix = "subscribe_overdue_";

    private static ConcurrentHashMap<Integer, SubscribeInfo> catalogMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, SubscribeInfo> mobilePositionMap = new ConcurrentHashMap<>();


    public void putCatalogSubscribe(Integer platformId, SubscribeInfo subscribeInfo) {
        catalogMap.put(platformId, subscribeInfo);
        if (subscribeInfo.getExpires() > 0) {
            // 添加订阅到期
            String taskOverdueKey = taskOverduePrefix +  "catalog_" + platformId;
            // 添加任务处理订阅过期
            dynamicTask.startDelay(taskOverdueKey, () -> removeCatalogSubscribe(platformId),
                    subscribeInfo.getExpires() * 1000);
        }
    }

    public SubscribeInfo getCatalogSubscribe(Integer platformId) {
        return catalogMap.get(platformId);
    }

    public void removeCatalogSubscribe(Integer platformId) {

        catalogMap.remove(platformId);
        String taskOverdueKey = taskOverduePrefix +  "catalog_" + platformId;
        Runnable runnable = dynamicTask.get(taskOverdueKey);
        if (runnable instanceof ISubscribeTask) {
            ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
            subscribeTask.stop(null);
        }
        // 添加任务处理订阅过期
        dynamicTask.stop(taskOverdueKey);
    }

    public void putMobilePositionSubscribe(Integer platformId, SubscribeInfo subscribeInfo) {
        mobilePositionMap.put(platformId, subscribeInfo);
        String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetting.getServerId() + "MobilePosition_" + platformId;
        // 添加任务处理GPS定时推送
        dynamicTask.startCron(key, new MobilePositionSubscribeHandlerTask(platformId),
                subscribeInfo.getGpsInterval() * 1000);
        String taskOverdueKey = taskOverduePrefix +  "MobilePosition_" + platformId;
        if (subscribeInfo.getExpires() > 0) {
            // 添加任务处理订阅过期
            dynamicTask.startDelay(taskOverdueKey, () -> {
                        removeMobilePositionSubscribe(platformId);
                    },
                    subscribeInfo.getExpires() * 1000);
        }
    }

    public SubscribeInfo getMobilePositionSubscribe(Integer platformId) {
        return mobilePositionMap.get(platformId);
    }

    public void removeMobilePositionSubscribe(Integer platformId) {
        mobilePositionMap.remove(platformId);
        String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetting.getServerId() + "MobilePosition_" + platformId;
        // 结束任务处理GPS定时推送
        dynamicTask.stop(key);
        String taskOverdueKey = taskOverduePrefix +  "MobilePosition_" + platformId;
        Runnable runnable = dynamicTask.get(taskOverdueKey);
        if (runnable instanceof ISubscribeTask) {
            ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
            subscribeTask.stop(null);
        }
        // 添加任务处理订阅过期
        dynamicTask.stop(taskOverdueKey);
    }

    public List<Integer> getAllCatalogSubscribePlatform() {
        List<Integer> platforms = new ArrayList<>();
        if(!catalogMap.isEmpty()) {
            platforms.addAll(catalogMap.keySet());
        }
        return platforms;
    }

    public void removeAllSubscribe(Integer platformId) {
        removeMobilePositionSubscribe(platformId);
        removeCatalogSubscribe(platformId);
    }
}
