package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cz.data.viid.fe.domain.APEDeviceQuery;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.APEDevice;

public interface APEDeviceService extends IService<APEDevice> {

    Page<APEDevice> page(APEDeviceQuery request);

    boolean saveDevice(APEDevice device);

    boolean updateDevice(APEDevice device);

    void deviceStatus(String deviceId, Constants.DeviceStatus status);
}
