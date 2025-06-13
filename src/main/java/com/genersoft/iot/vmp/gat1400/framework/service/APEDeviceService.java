package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.fontend.domain.APEDeviceQuery;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.APEDevice;


public interface APEDeviceService extends IService<APEDevice> {

    Page<APEDevice> page(APEDeviceQuery request);

    boolean saveDevice(APEDevice device);

    boolean updateDevice(APEDevice device);

    void deviceStatus(String deviceId, Constants.DeviceStatus status);
}
