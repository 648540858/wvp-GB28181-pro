package com.genersoft.iot.vmp.gb28181.event.platformNotRegister;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

@Component
public class PlatformCycleRegisterEventLister implements ApplicationListener<PlatformCycleRegisterEvent> {

    private final static Logger logger = LoggerFactory.getLogger(PlatformCycleRegisterEventLister.class);

    @Autowired
    private IVideoManagerStorage storager;
    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;
    @Autowired
    private DynamicTask dynamicTask;

    @Override
    public void onApplicationEvent(PlatformCycleRegisterEvent event) {
        logger.info("上级平台周期注册事件");
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformGbID());
        if (parentPlatform == null) {
            logger.info("[ 平台未注册事件 ] 平台已经删除!!! 平台国标ID：" + event.getPlatformGbID());
            return;
        }
        String taskKey = "platform-cycle-register" + parentPlatform.getServerGBId();;
        SipSubscribe.Event okEvent = (responseEvent)->{
            dynamicTask.stop(taskKey);
        };
        dynamicTask.startCron(taskKey, ()->{
            logger.info("[平台注册]再次向平台注册，平台国标ID：" + event.getPlatformGbID());
            sipCommanderFroPlatform.register(parentPlatform, null, okEvent);
        }, Integer.parseInt(parentPlatform.getExpires())* 1000);
    }
}
