package com.genersoft.iot.vmp.gb28181.event.platformNotRegister;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @description: 平台未注册事件,来源有二:
 *               1、平台新添加
 *               2、平台心跳超时
 * @author: panll
 * @date: 2020年11月24日 10:00
 */
@Component
public class PlatformNotRegisterEventLister implements ApplicationListener<PlatformNotRegisterEvent> {

    private final static Logger logger = LoggerFactory.getLogger(PlatformNotRegisterEventLister.class);

    @Autowired
    private IVideoManagerStorage storager;
    @Autowired
    private IRedisCatchStorage redisCatchStorage;
    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private SipConfig config;

    @Autowired
    private DynamicTask dynamicTask;

    // @Autowired
    // private RedisUtil redis;

    @Override
    public void onApplicationEvent(PlatformNotRegisterEvent event) {

        logger.info("[ 平台未注册事件 ]平台国标ID：" + event.getPlatformGbID());

        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformGbID());
        if (parentPlatform == null) {
            logger.info("[ 平台未注册事件 ] 平台已经删除!!! 平台国标ID：" + event.getPlatformGbID());
            return;
        }
        // 查询是否有推流， 如果有则都停止
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(event.getPlatformGbID());
        logger.info("[ 平台未注册事件 ] 停止[ {} ]的所有推流size", sendRtpItems.size());
        if (sendRtpItems != null && sendRtpItems.size() > 0) {
            logger.info("[ 平台未注册事件 ] 停止[ {} ]的所有推流", event.getPlatformGbID());
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                redisCatchStorage.deleteSendRTPServer(event.getPlatformGbID(), sendRtpItem.getChannelId(), null, null);
                MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Map<String, Object> param = new HashMap<>();
                param.put("vhost", "__defaultVhost__");
                param.put("app", sendRtpItem.getApp());
                param.put("stream", sendRtpItem.getStreamId());
                zlmrtpServerFactory.stopSendRtpStream(mediaInfo, param);
            }

        }
        String taskKey = "platform-not-register-" + parentPlatform.getServerGBId();
        SipSubscribe.Event okEvent = (responseEvent)->{
            dynamicTask.stop(taskKey);
        };
        dynamicTask.startCron(taskKey, ()->{
            logger.info("[平台注册]再次向平台注册，平台国标ID：" + event.getPlatformGbID());
            sipCommanderFroPlatform.register(parentPlatform, null, okEvent);
        }, config.getRegisterTimeInterval()* 1000);
    }
}
