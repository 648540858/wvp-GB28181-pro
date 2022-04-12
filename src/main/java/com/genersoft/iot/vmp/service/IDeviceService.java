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
     * 设置通道同步状态
     * @param deviceId 设备ID
     */
    void setChannelSyncReady(String deviceId);

    /**
     * 设置同步结束
     * @param deviceId 设备ID
     * @param errorMsg 错误信息
     */
    void setChannelSyncEnd(String deviceId, String errorMsg);
}
