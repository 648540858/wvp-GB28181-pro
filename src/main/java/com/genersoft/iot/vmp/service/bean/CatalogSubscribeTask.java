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
            try {
                rootElement = XmlUtil.getRootElement(event.getResponse().getRawContent(), "gb2312");
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            Element resultElement = rootElement.element("Result");
            String result = resultElement.getText();
            if (result.toUpperCase().equals("OK")){
                // 成功
                logger.info("目录订阅成功： {}", device.getDeviceId());
            }else {
                // 失败
                logger.info("目录订阅失败： {}-{}", device.getDeviceId(), result);
            }

        },eventResult -> {
            // 失败
            logger.warn("目录订阅失败： {}-信令发送失败", device.getDeviceId());
        });
    }
}
