package com.genersoft.iot.vmp.gb28181.task.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import javax.sip.DialogState;
import java.util.List;

/**
 * 向已经订阅(移动位置)的上级发送MobilePosition消息
 * @author lin
 */
public class MobilePositionSubscribeHandlerTask implements ISubscribeTask {


    private IPlatformService platformService;
    private String platformId;


    public MobilePositionSubscribeHandlerTask(String platformId) {
        this.platformService = SpringBeanFactory.getBean("platformServiceImpl");
        this.platformId = platformId;
    }

    @Override
    public void run() {
        platformService.sendNotifyMobilePosition(this.platformId);
    }

    @Override
    public void stop() {

    }
}
