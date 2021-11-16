package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.bean.CatalogSubscribeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceServiceImpl implements IDeviceService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    @Autowired
    private DynamicTask dynamicTask;
;

    @Autowired
    private ISIPCommander sipCommander;

    @Override
    public boolean addCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        // 添加目录订阅
        CatalogSubscribeTask catalogSubscribeTask = new CatalogSubscribeTask(device, sipCommander);
        catalogSubscribeTask.run();
        // 提前开始刷新订阅
        // TODO 暂时关闭目录订阅的定时刷新，直到此功能完善
//        String cron = getCron(device.getSubscribeCycleForCatalog() - 60);
//        dynamicTask.startCron(device.getDeviceId(), catalogSubscribeTask, cron);
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        dynamicTask.stopCron(device.getDeviceId());
        return true;
    }

    public String getCron(int time) {
        if (time <= 59) {
            return "0/" + time +" * * * * ?";
        }else if (time <= 60* 59) {
            int minute = time/(60);
            return "0 0/" + minute +" * * * ?";
        }else if (time <= 60* 60* 59) {
            int hour = time/(60*60);
            return "0 0 0/" + hour +" * * ?";
        }else {
            return "0 0/10 * * * ?";
        }
    }
}
