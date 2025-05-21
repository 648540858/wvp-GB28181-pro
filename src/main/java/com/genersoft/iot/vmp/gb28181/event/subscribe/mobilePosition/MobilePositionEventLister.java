package com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class MobilePositionEventLister implements ApplicationListener<MobilePositionEvent> {

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Override
    public void onApplicationEvent(MobilePositionEvent event) {
        if (event.getMobilePosition().getChannelId() == 0) {
            return;
        }
        List<Platform> allPlatforms = platformService.queryAll();
        // 获取所用订阅
        List<String> platforms = subscribeHolder.getAllMobilePositionSubscribePlatform(allPlatforms);
        if (platforms.isEmpty()) {
            return;
        }
        List<Platform> platformsForGB = platformChannelService.queryPlatFormListByChannelDeviceId(event.getMobilePosition().getChannelId(), platforms);

        for (Platform platform : platformsForGB) {
            if (log.isDebugEnabled()){
                log.debug("[向上级发送MobilePosition] 通道：{}，平台：{}， 位置： {}:{}", event.getMobilePosition().getChannelId(),
                        platform.getServerGBId(), event.getMobilePosition().getLongitude(), event.getMobilePosition().getLatitude());
            }
            SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId());
            try {
                GPSMsgInfo gpsMsgInfo = GPSMsgInfo.getInstance(event.getMobilePosition());
                // 获取通道编号
                CommonGBChannel commonGBChannel = platformChannelService.queryChannelByPlatformIdAndChannelId(platform.getId(), event.getMobilePosition().getChannelId());
                sipCommanderForPlatform.sendNotifyMobilePosition(platform, gpsMsgInfo, commonGBChannel,
                        subscribe);
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
            }
        }
    }
}
