package com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 移动位置通知消息转发
 */
@Component
public class MobilePositionEventLister implements ApplicationListener<MobilePositionEvent> {

    private final static Logger logger = LoggerFactory.getLogger(MobilePositionEventLister.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Override
    public void onApplicationEvent(MobilePositionEvent event) {
        // 获取所用订阅
        List<String> platforms = subscribeHolder.getAllMobilePositionSubscribePlatform();
        if (platforms.isEmpty()) {
            return;
        }
        List<ParentPlatform> parentPlatformsForGB = storager.queryPlatFormListForGBWithGBId(event.getMobilePosition().getChannelId(), platforms);

        for (ParentPlatform platform : parentPlatformsForGB) {
            logger.debug("[向上级发送MobilePosition] 通道：{}，平台：{}， 位置： {}:{}", event.getMobilePosition().getChannelId(),
                    platform.getServerGBId(), event.getMobilePosition().getLongitude(), event.getMobilePosition().getLatitude());
            SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId());
            try {
                sipCommanderFroPlatform.sendNotifyMobilePosition(platform, GPSMsgInfo.getInstance(event.getMobilePosition()),
                        subscribe);
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
            }
        }

    }
}
 