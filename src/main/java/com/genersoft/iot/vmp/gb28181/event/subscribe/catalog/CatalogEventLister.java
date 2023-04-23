package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * catalog事件
 */
@Component
public class CatalogEventLister implements ApplicationListener<CatalogEvent> {

    private final static Logger logger = LoggerFactory.getLogger(CatalogEventLister.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private IGbStreamService gbStreamService;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void onApplicationEvent(CatalogEvent event) {
        SubscribeInfo subscribe = null;
        ParentPlatform parentPlatform = null;

        Map<String, List<ParentPlatform>> parentPlatformMap = new HashMap<>();
        if (!ObjectUtils.isEmpty(event.getPlatformId())) {
            subscribe = subscribeHolder.getCatalogSubscribe(event.getPlatformId());
            if (subscribe == null) {
                return;
            }
            parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformId());
            if (parentPlatform != null && !parentPlatform.isStatus()) {
                return;
            }

        }else {
            // 获取所用订阅
            List<String> platforms = subscribeHolder.getAllCatalogSubscribePlatform();
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
                        if (gbStream == null || ObjectUtils.isEmpty(gbStream.getGbId())) {
                            continue;
                        }
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
                            if (gbStream.getStreamType().equals("push") && !userSetting.isUsePushingAsStatus()) {
                                continue;
                            }
                            DeviceChannel deviceChannelByStream = gbStreamService.getDeviceChannelListByStream(gbStream, gbStream.getCatalogId(), parentPlatform);
                            deviceChannelList.add(deviceChannelByStream);
                        }
                    }
                    if (deviceChannelList.size() > 0) {
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), deviceChannelList.size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), parentPlatform, deviceChannelList, subscribe, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                        }
                    }
                }else if (parentPlatformMap.keySet().size() > 0) {
                    for (String gbId : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(gbId);
                        if (parentPlatforms != null && parentPlatforms.size() > 0) {
                            for (ParentPlatform platform : parentPlatforms) {
                                SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                                if (subscribeInfo == null) {
                                    continue;
                                }
                                logger.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), gbId);
                                List<DeviceChannel> deviceChannelList = new ArrayList<>();
                                DeviceChannel deviceChannel = new DeviceChannel();
                                deviceChannel.setChannelId(gbId);
                                deviceChannelList.add(deviceChannel);
                                try {
                                    sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), platform, deviceChannelList, subscribeInfo, null);
                                } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                         IllegalAccessException e) {
                                    logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                                }
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
                            deviceChannelList.add(
                                    gbStreamService.getDeviceChannelListByStreamWithStatus(gbStream, gbStream.getCatalogId(), parentPlatform));
                        }
                    }
                    if (deviceChannelList.size() > 0) {
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), deviceChannelList.size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), parentPlatform, deviceChannelList, subscribe, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                        }
                    }
                }else if (parentPlatformMap.keySet().size() > 0) {
                    for (String gbId : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(gbId);
                        if (parentPlatforms != null && parentPlatforms.size() > 0) {
                            for (ParentPlatform platform : parentPlatforms) {
                                SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                                if (subscribeInfo == null) {
                                    continue;
                                }
                                logger.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), gbId);
                                List<DeviceChannel> deviceChannelList = new ArrayList<>();
                                DeviceChannel deviceChannel = storager.queryChannelInParentPlatform(platform.getServerGBId(), gbId);
                                deviceChannelList.add(deviceChannel);
                                GbStream gbStream = storager.queryStreamInParentPlatform(platform.getServerGBId(), gbId);
                                if(gbStream != null){
                                    DeviceChannel deviceChannelByStream = gbStreamService.getDeviceChannelListByStreamWithStatus(gbStream, gbStream.getCatalogId(), platform);
                                    deviceChannelList.add(deviceChannelByStream);
                                }
                                try {
                                    sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), platform, deviceChannelList, subscribeInfo, null);
                                } catch (InvalidArgumentException | ParseException | NoSuchFieldException |
                                         SipException | IllegalAccessException e) {
                                    logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                                }
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
 