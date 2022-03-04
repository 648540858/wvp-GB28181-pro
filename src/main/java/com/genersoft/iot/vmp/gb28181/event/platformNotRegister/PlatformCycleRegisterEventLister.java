package com.genersoft.iot.vmp.gb28181.event.platformNotRegister;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
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
    private IVideoManagerStorager storager;
    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Override
    public void onApplicationEvent(PlatformCycleRegisterEvent event) {
        logger.info("上级平台周期注册事件");
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformGbID());
        if (parentPlatform == null) {
            logger.info("[ 平台未注册事件 ] 平台已经删除!!! 平台国标ID：" + event.getPlatformGbID());
            return;
        }
        Timer timer = new Timer();
        SipSubscribe.Event okEvent = (responseEvent)->{
            timer.cancel();
        };
        sipCommanderFroPlatform.register(parentPlatform, null, okEvent);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("[平台注册]再次向平台注册，平台国标ID：" + event.getPlatformGbID());
                sipCommanderFroPlatform.register(parentPlatform, null, okEvent);
            }
        }, 15*1000 ,Long.parseLong(parentPlatform.getExpires())* 1000);
    }
}
