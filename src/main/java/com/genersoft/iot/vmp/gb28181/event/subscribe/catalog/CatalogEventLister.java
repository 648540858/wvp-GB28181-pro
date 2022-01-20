package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.platformNotRegister.PlatformNotRegisterEvent;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * catalog事件
 */
@Component
public class CatalogEventLister implements ApplicationListener<CatalogEvent> {

    private final static Logger logger = LoggerFactory.getLogger(CatalogEventLister.class);

    @Autowired
    private IVideoManagerStorager storager;
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
    private UserSetup userSetup;

    @Autowired
    private IGbStreamService gbStreamService;

    @Override
    public void onApplicationEvent(CatalogEvent event) {
        SubscribeInfo subscribe = null;
        ParentPlatform parentPlatform = null;

        Map<String, List<ParentPlatform>> parentPlatformMap = new HashMap<>();
        if (event.getPlatformId() != null) {
            parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformId());
            if (!parentPlatform.isStatus())return;
            String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() +  "_Catalog_" + event.getPlatformId();
            subscribe = redisCatchStorage.getSubscribe(key);
            if (subscribe == null) return;
        }else {
            // 获取所用订阅
            List<String> platforms = redisCatchStorage.getAllSubscribePlatform();
            if (event.getDeviceChannels() != null) {
                if (platforms.size() > 0) {
                    for (DeviceChannel deviceChannel : event.getDeviceChannels()) {
                        List<ParentPlatform> parentPlatformsForGB = storager.queryPlatFormListForGBWithGBId(deviceChannel.getChannelId(), platforms);
                        parentPlatformMap.put(deviceChannel.getChannelId(), parentPlatformsForGB);
                    }
                }
            }else if (event.getGbStreams() != null) {
                if (platforms.size() > 0) {
                    for (GbStream gbStream : event.getGbStreams()) {
                        if (gbStream == null || StringUtils.isEmpty(gbStream.getGbId())) continue;
                        List<ParentPlatform> parentPlatformsForGB = storager.queryPlatFormListForStreamWithGBId(gbStream.getApp(),gbStream.getStream(), platforms);
                        parentPlatformMap.put(gbStream.getGbId(), parentPlatformsForGB);
                    }
                }
            }
        }
        switch (event.getType()) {
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
            case CatalogEvent.DEL:

                if (parentPlatform != null || subscribe != null) {
                    List<DeviceChannel> deviceChannelList = new ArrayList<>();
                    if (event.getDeviceChannels() != null) {
                        deviceChannelList.addAll(event.getDeviceChannels());
                    }
                    if (event.getGbStreams() != null && event.getGbStreams().size() > 0){
                        for (GbStream gbStream : event.getGbStreams()) {
                            DeviceChannel deviceChannelByStream = gbStreamService.getDeviceChannelListByStream(gbStream, gbStream.getCatalogId(), parentPlatform.getDeviceGBId());
                            deviceChannelList.add(deviceChannelByStream);
                        }
                    }
                    if (deviceChannelList.size() > 0) {
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), deviceChannelList.size());
                        sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), parentPlatform, deviceChannelList, subscribe);
                    }
                }else if (parentPlatformMap.keySet().size() > 0) {
                    for (String gbId : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(gbId);
                        if (parentPlatforms != null && parentPlatforms.size() > 0) {
                            for (ParentPlatform platform : parentPlatforms) {
                                String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() +  "_Catalog_" + platform.getServerGBId();
                                SubscribeInfo subscribeInfo = redisCatchStorage.getSubscribe(key);
                                if (subscribeInfo == null) continue;
                                logger.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), gbId);
                                List<DeviceChannel> deviceChannelList = new ArrayList<>();
                                DeviceChannel deviceChannel = new DeviceChannel();
                                deviceChannel.setChannelId(gbId);
                                deviceChannelList.add(deviceChannel);
                                sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), platform, deviceChannelList, subscribeInfo);
                            }
                        }
                    }
                }
                break;
            case CatalogEvent.VLOST:
                break;
            case CatalogEvent.DEFECT:
                break;
            case CatalogEvent.ADD:
            case CatalogEvent.UPDATE:
                if (parentPlatform != null || subscribe != null) {
                     List<DeviceChannel> deviceChannelList = new ArrayList<>();
                     if (event.getDeviceChannels() != null) {
                         deviceChannelList.addAll(event.getDeviceChannels());
                     }
                    if (event.getGbStreams() != null && event.getGbStreams().size() > 0){
                        for (GbStream gbStream : event.getGbStreams()) {
                            DeviceChannel deviceChannelByStream = gbStreamService.getDeviceChannelListByStream(gbStream, gbStream.getCatalogId(), parentPlatform.getDeviceGBId());
                            deviceChannelList.add(deviceChannelByStream);
                        }
                    }
                    if (deviceChannelList.size() > 0) {
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), deviceChannelList.size());
                        sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), parentPlatform, deviceChannelList, subscribe);
                    }
                }else if (parentPlatformMap.keySet().size() > 0) {
                    for (String gbId : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(gbId);
                        if (parentPlatforms != null && parentPlatforms.size() > 0) {
                            for (ParentPlatform platform : parentPlatforms) {
                                String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetup.getServerId() + "_Catalog_" + platform.getServerGBId();
                                SubscribeInfo subscribeInfo = redisCatchStorage.getSubscribe(key);
                                if (subscribeInfo == null) continue;
                                logger.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), gbId);
                                List<DeviceChannel> deviceChannelList = new ArrayList<>();
                                DeviceChannel deviceChannel = storager.queryChannelInParentPlatform(platform.getServerGBId(), gbId);
                                deviceChannelList.add(deviceChannel);
                                GbStream gbStream = storager.queryStreamInParentPlatform(platform.getServerGBId(), gbId);
                                DeviceChannel deviceChannelByStream = gbStreamService.getDeviceChannelListByStream(gbStream, gbStream.getCatalogId(), platform.getDeviceGBId());
                                deviceChannelList.add(deviceChannelByStream);
                                sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), platform, deviceChannelList, subscribeInfo);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
}
