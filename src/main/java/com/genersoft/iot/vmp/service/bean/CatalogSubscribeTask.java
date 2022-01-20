package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.ResponseEvent;

public class CatalogSubscribeTask implements Runnable{
    private final Logger logger = LoggerFactory.getLogger(CatalogSubscribeTask.class);
    private  Device device;
    private  ISIPCommander sipCommander;

    public CatalogSubscribeTask(Device device, ISIPCommander sipCommander) {
        this.device = device;
        this.sipCommander = sipCommander;
    }

    @Override
    public void run() {
        sipCommander.catalogSubscribe(device, eventResult -> {
            ResponseEvent event = (ResponseEvent) eventResult.event;
            Element rootElement = null;
            if (event.getResponse().getRawContent() != null) {
                // 成功
                logger.info("[目录订阅]成功： {}", device.getDeviceId());
            }else {
                // 成功
                logger.info("[目录订阅]成功： {}", device.getDeviceId());
            }
        },eventResult -> {
            // 失败
            logger.warn("[目录订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
        });
    }
}
