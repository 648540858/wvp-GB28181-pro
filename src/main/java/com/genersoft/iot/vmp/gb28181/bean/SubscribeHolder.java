package com.genersoft.iot.vmp.gb28181.bean;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SubscribeHolder {

    private static ConcurrentHashMap<String, SubscribeInfo> catalogMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, SubscribeInfo> mobilePositionMap = new ConcurrentHashMap<>();


    public void putCatalogSubscribe(String platformId, SubscribeInfo subscribeInfo) {
        catalogMap.put(platformId, subscribeInfo);
    }

    public SubscribeInfo getCatalogSubscribe(String platformId) {
        return catalogMap.get(platformId);
    }

    public void removeCatalogSubscribe(String platformId) {
        catalogMap.remove(platformId);
    }

    public void putMobilePositionSubscribe(String platformId, SubscribeInfo subscribeInfo) {
        mobilePositionMap.put(platformId, subscribeInfo);
    }

    public SubscribeInfo getMobilePositionSubscribe(String platformId) {
        return mobilePositionMap.get(platformId);
    }

    public void removeMobilePositionSubscribe(String platformId) {
        mobilePositionMap.remove(platformId);
    }
}
