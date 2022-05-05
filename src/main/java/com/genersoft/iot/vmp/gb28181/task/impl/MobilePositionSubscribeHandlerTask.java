package com.genersoft.iot.vmp.gb28181.task.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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

    private Logger logger = LoggerFactory.getLogger(MobilePositionSubscribeHandlerTask.class);

    private IRedisCatchStorage redisCatchStorage;
    private IVideoManagerStorage storager;
    private ISIPCommanderForPlatform sipCommanderForPlatform;
    private SubscribeHolder subscribeHolder;
    private ParentPlatform platform;

    private String sn;
    private String key;

    public MobilePositionSubscribeHandlerTask(IRedisCatchStorage redisCatchStorage,
                                              ISIPCommanderForPlatform sipCommanderForPlatform,
                                              IVideoManagerStorage storager,
                                              String platformId,
                                              String sn,
                                              String key,
                                              SubscribeHolder subscribeInfo,
                                              DynamicTask dynamicTask) {
        this.redisCatchStorage = redisCatchStorage;
        this.storager = storager;
        this.platform = storager.queryParentPlatByServerGBId(platformId);
        this.sn = sn;
        this.key = key;
        this.sipCommanderForPlatform = sipCommanderForPlatform;
        this.subscribeHolder = subscribeInfo;
    }

    @Override
    public void run() {

        if (platform == null) {
            return;
        }
        SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId());
        if (subscribe != null) {

//            if (!parentPlatform.isStatus()) {
//                logger.info("发送订阅时发现平台已经离线：{}", platformId);
//                return;
//            }
            // TODO 暂时只处理视频流的回复,后续增加对国标设备的支持
            List<GbStream> gbStreams = storager.queryGbStreamListInPlatform(platform.getServerGBId());
            if (gbStreams.size() == 0) {
                logger.info("发送订阅时发现平台已经没有关联的直播流：{}", platform.getServerGBId());
                return;
            }
            for (GbStream gbStream : gbStreams) {
                String gbId = gbStream.getGbId();
                GPSMsgInfo gpsMsgInfo = redisCatchStorage.getGpsMsgInfo(gbId);
                if (gpsMsgInfo != null) { // 无最新位置不发送
                    logger.info("无最新位置不发送");
                    // 经纬度都为0不发送
                    if (gpsMsgInfo.getLng() == 0 && gpsMsgInfo.getLat() == 0) {
                        continue;
                    }
                    // 发送GPS消息
                    sipCommanderForPlatform.sendNotifyMobilePosition(platform, gpsMsgInfo, subscribe);
                }
            }
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public DialogState getDialogState() {
        return null;
    }
}
