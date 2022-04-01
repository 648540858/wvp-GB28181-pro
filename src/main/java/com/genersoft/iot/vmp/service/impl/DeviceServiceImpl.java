package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.bean.CatalogSubscribeTask;
import com.genersoft.iot.vmp.service.bean.MobilePositionSubscribeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 设备业务（目录订阅）
 */
@Service
public class DeviceServiceImpl implements IDeviceService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ISIPCommander sipCommander;

    @Override
    public boolean addCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        if (dynamicTask.contains(device.getDeviceId() + "catalog")) {
            // 存在则停止现有的，开启新的
            dynamicTask.stop(device.getDeviceId() + "catalog");
        }
        logger.info("[添加目录订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        CatalogSubscribeTask catalogSubscribeTask = new CatalogSubscribeTask(device, sipCommander);
        catalogSubscribeTask.run();
        // 提前开始刷新订阅
        int subscribeCycleForCatalog = device.getSubscribeCycleForCatalog();
        // 设置最小值为30
        subscribeCycleForCatalog = Math.max(subscribeCycleForCatalog, 30);
        dynamicTask.startCron(device.getDeviceId() + "catalog", catalogSubscribeTask, subscribeCycleForCatalog - 5);
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("移除目录订阅: {}", device.getDeviceId());
        dynamicTask.stop(device.getDeviceId() + "catalog");
        device.setSubscribeCycleForCatalog(0);
        sipCommander.catalogSubscribe(device, null, null);
        return true;
    }

    @Override
    public boolean addMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForMobilePosition() < 0) {
            return false;
        }
        if (dynamicTask.contains(device.getDeviceId() + "mobile_position")) {
            // 存在则停止现有的，开启新的
            dynamicTask.stop(device.getDeviceId() + "mobile_position");
        }
        logger.info("[添加移动位置订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        MobilePositionSubscribeTask mobilePositionSubscribeTask = new MobilePositionSubscribeTask(device, sipCommander);
        mobilePositionSubscribeTask.run();
        // 提前开始刷新订阅
        int subscribeCycleForCatalog = device.getSubscribeCycleForCatalog();
        // 设置最小值为30
        subscribeCycleForCatalog = Math.max(subscribeCycleForCatalog, 30);
        dynamicTask.startCron(device.getDeviceId() + "mobile_position" , mobilePositionSubscribeTask, subscribeCycleForCatalog - 5);
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("移除移动位置订阅: {}", device.getDeviceId());
        dynamicTask.stop(device.getDeviceId() + "mobile_position");
        device.setSubscribeCycleForCatalog(0);
        sipCommander.mobilePositionSubscribe(device, null, null);
        return true;
    }
}
