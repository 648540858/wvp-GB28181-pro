package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.ResponseEvent;

public class MobilePositionSubscribeTask implements Runnable{
    private final Logger logger = LoggerFactory.getLogger(MobilePositionSubscribeTask.class);
    private  Device device;
    private  ISIPCommander sipCommander;

    public MobilePositionSubscribeTask(Device device, ISIPCommander sipCommander) {
        this.device = device;
        this.sipCommander = sipCommander;
    }

    @Override
    public void run() {
        sipCommander.mobilePositionSubscribe(device, eventResult -> {
            ResponseEvent event = (ResponseEvent) eventResult.event;
            Element rootElement = null;
            if (event.getResponse().getRawContent() != null) {
                // 成功
                logger.info("[移动位置订阅]成功： {}", device.getDeviceId());
            }else {
                // 成功
                logger.info("[移动位置订阅]成功： {}", device.getDeviceId());
            }
        },eventResult -> {
            // 失败
            logger.warn("[移动位置订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
        });
    }
}
