package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.Device;

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

}
