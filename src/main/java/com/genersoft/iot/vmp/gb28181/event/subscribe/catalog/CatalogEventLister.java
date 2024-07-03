package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class CatalogEventLister implements ApplicationListener<CatalogEvent> {

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void onApplicationEvent(CatalogEvent event) {
        SubscribeInfo subscribe = null;
        ParentPlatform parentPlatform = null;

        Map<String, List<ParentPlatform>> parentPlatformMap = new HashMap<>();
        Map<String, CommonGBChannel> channelMap = new HashMap<>();
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
            if (event.getChannels() != null) {
                if (platforms.size() > 0) {
                    for (CommonGBChannel deviceChannel : event.getChannels()) {
                        List<ParentPlatform> parentPlatformsForGB = storager.queryPlatFormListForGBWithGBId(deviceChannel.getGbDeviceId(), platforms);
                        parentPlatformMap.put(deviceChannel.getGbDeviceId(), parentPlatformsForGB);
                        channelMap.put(deviceChannel.getGbDeviceId(), deviceChannel);
                    }
                }
            }
        }
        switch (event.getType()) {
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
            case CatalogEvent.DEL:

                if (parentPlatform != null || subscribe != null) {
                    List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                    if (event.getChannels() != null) {
                        deviceChannelList.addAll(event.getChannels());
                    }
                    if (deviceChannelList.size() > 0) {
                        log.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), deviceChannelList.size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), parentPlatform, deviceChannelList, subscribe, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
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
                                log.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), gbId);
                                List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                                CommonGBChannel deviceChannel = new CommonGBChannel();
                                deviceChannel.setGbDeviceId(gbId);
                                deviceChannelList.add(deviceChannel);
                                try {
                                    sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), platform, deviceChannelList, subscribeInfo, null);
                                } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                         IllegalAccessException e) {
                                    log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
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
                     List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                     if (event.getChannels() != null) {
                         deviceChannelList.addAll(event.getChannels());
                     }
                    if (!deviceChannelList.isEmpty()) {
                        log.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), deviceChannelList.size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), parentPlatform, deviceChannelList, subscribe, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                        }
                    }
                }else if (!parentPlatformMap.keySet().isEmpty()) {
                    for (String gbId : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(gbId);
                        if (parentPlatforms != null && !parentPlatforms.isEmpty()) {
                            for (ParentPlatform platform : parentPlatforms) {
                                SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                                if (subscribeInfo == null) {
                                    continue;
                                }
                                log.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), gbId);
                                List<CommonGBChannel> channelList = new ArrayList<>();
                                CommonGBChannel deviceChannel = channelMap.get(gbId);
                                channelList.add(deviceChannel);
                                try {
                                    sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), platform, channelList, subscribeInfo, null);
                                } catch (InvalidArgumentException | ParseException | NoSuchFieldException |
                                         SipException | IllegalAccessException e) {
                                    log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
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
 