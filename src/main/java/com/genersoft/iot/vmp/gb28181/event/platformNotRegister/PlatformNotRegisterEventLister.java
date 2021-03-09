package com.genersoft.iot.vmp.gb28181.event.platformNotRegister;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @Description: 平台未注册事件,来源有二:
 *               1、平台新添加
 *               2、平台心跳超时
 * @author: panll
 * @date: 2020年11月24日 10:00
 */
@Component
public class PlatformNotRegisterEventLister implements ApplicationListener<PlatformNotRegisterEvent> {

    private final static Logger logger = LoggerFactory.getLogger(PlatformNotRegisterEventLister.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    // @Autowired
    // private RedisUtil redis;

    @Override
    public void onApplicationEvent(PlatformNotRegisterEvent event) {

        logger.debug("平台未注册事件触发，平台国标ID：" + event.getPlatformGbID());

        ParentPlatform parentPlatform = storager.queryParentPlatById(event.getPlatformGbID());
        if (parentPlatform == null) {
            logger.debug("平台未注册事件触发，但平台已经删除!!! 平台国标ID：" + event.getPlatformGbID());
            return;
        }
        sipCommanderFroPlatform.register(parentPlatform);
    }
}
