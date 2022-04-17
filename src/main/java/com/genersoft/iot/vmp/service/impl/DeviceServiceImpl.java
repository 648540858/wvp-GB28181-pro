package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.DialogState;

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

    @Autowired
    private CatalogResponseMessageHandler catalogResponseMessageHandler;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public boolean addCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        CatalogSubscribeTask task = (CatalogSubscribeTask)dynamicTask.get(device.getDeviceId() + "catalog");
        if (task != null && task.getDialogState() != null && task.getDialogState().equals(DialogState.CONFIRMED)) { // 已存在不需要再次添加
            return true;
        }
        logger.info("[添加目录订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        CatalogSubscribeTask catalogSubscribeTask = new CatalogSubscribeTask(device, sipCommander);
        // 提前开始刷新订阅
        int subscribeCycleForCatalog = Math.max(device.getSubscribeCycleForCatalog(),30);
        // 设置最小值为30
        dynamicTask.startCron(device.getDeviceId() + "catalog", catalogSubscribeTask, subscribeCycleForCatalog -1);
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("移除目录订阅: {}", device.getDeviceId());
        dynamicTask.stop(device.getDeviceId() + "catalog");
        return true;
    }

    @Override
    public boolean addMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForMobilePosition() < 0) {
            return false;
        }
        logger.info("[添加移动位置订阅] 设备{}", device.getDeviceId());
        MobilePositionSubscribeTask task = (MobilePositionSubscribeTask)dynamicTask.get(device.getDeviceId() + "mobile_position");
        if (task != null &&  task.getDialogState() != null && task.getDialogState().equals(DialogState.CONFIRMED)) { // 已存在不需要再次添加
            return true;
        }
        // 添加目录订阅
        MobilePositionSubscribeTask mobilePositionSubscribeTask = new MobilePositionSubscribeTask(device, sipCommander);
        // 提前开始刷新订阅
        // 设置最小值为30
        int subscribeCycleForCatalog = Math.max(device.getSubscribeCycleForMobilePosition(),30);
        dynamicTask.startCron(device.getDeviceId() + "mobile_position" , mobilePositionSubscribeTask, subscribeCycleForCatalog -1 );
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("移除移动位置订阅: {}", device.getDeviceId());
        dynamicTask.stop(device.getDeviceId() + "mobile_position");
        return true;
    }

    @Override
    public SyncStatus getChannelSyncStatus(String deviceId) {
        return catalogResponseMessageHandler.getChannelSyncProgress(deviceId);
    }

    @Override
    public void sync(Device device) {
        if (catalogResponseMessageHandler.getChannelSyncProgress(device.getDeviceId()) != null) {
            logger.info("开启同步时发现同步已经存在");
            return;
        }
        int sn = (int)((Math.random()*9+1)*100000);
        catalogResponseMessageHandler.setChannelSyncReady(device, sn);
        sipCommander.catalogQuery(device, sn, event -> {
            String errorMsg = String.format("同步通道失败，错误码： %s, %s", event.statusCode, event.msg);
            catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), errorMsg);
        });
    }
}
