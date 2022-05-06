package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;

/**
 * 设备相关业务处理
 */
public interface IDeviceService {

    /**
     * 添加目录订阅
     * @param device 设备信息
     * @return
     */
    boolean addCatalogSubscribe(Device device);

    /**
     * 移除目录订阅
     * @param device 设备信息
     * @return
     */
    boolean removeCatalogSubscribe(Device device);

    /**
     * 添加移动位置订阅
     * @param device 设备信息
     * @return
     */
    boolean addMobilePositionSubscribe(Device device);

    /**
     * 移除移动位置订阅
     * @param device 设备信息
     * @return
     */
    boolean removeMobilePositionSubscribe(Device device);

    /**
     * 移除移动位置订阅
     * @param deviceId 设备ID
     * @return
     */
    SyncStatus getChannelSyncStatus(String deviceId);

    /**
     * 查看是否仍在同步
     * @param deviceId 设备ID
     * @return
     */
    Boolean isSyncRunning(String deviceId);

    /**
     * 通道同步
     * @param device
     */
    void sync(Device device);
}
