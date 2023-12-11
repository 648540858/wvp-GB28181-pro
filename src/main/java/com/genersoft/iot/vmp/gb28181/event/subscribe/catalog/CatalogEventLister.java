package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.IPlatformService;
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
    private IPlatformChannelService platformChannelService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Override
    public void onApplicationEvent(CatalogEvent event) {
        SubscribeInfo subscribe = null;
        ParentPlatform parentPlatform = null;

        Map<CommonGbChannel, List<ParentPlatform>> parentPlatformMap = new HashMap<>();

        if (!ObjectUtils.isEmpty(event.getPlatformId())) {
            // 如果事件指定了要通知的上级，那么定向发给这个上级
            subscribe = subscribeHolder.getCatalogSubscribe(event.getPlatformId());
            if (subscribe == null) {
                return;
            }
            parentPlatform = platformService.query(event.getPlatformId());
            if (parentPlatform != null && !parentPlatform.isStatus()) {
                return;
            }

        }else {
            // 如果事件没有要通知的上级，那么需要自己查询到所有要通知的上级进行通知
            List<String> platforms = subscribeHolder.getAllCatalogSubscribePlatform();
            if (event.getChannels() != null) {
                if (!platforms.isEmpty()) {
                    for (CommonGbChannel channel : event.getChannels()) {
                        List<ParentPlatform> parentPlatformsForGB = platformChannelService.querySharePlatformListByChannelId(channel.getCommonGbId(), platforms);
                        parentPlatformMap.put(channel, parentPlatformsForGB);
                    }
                }
            }
        }
        switch (event.getType()) {
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
            case CatalogEvent.DEL:

                if (parentPlatform != null ) {

                    if (!event.getChannels().isEmpty()) {
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), event.getChannels().size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), parentPlatform, event.getChannels(), subscribe, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                        }
                    }
                }else if (!parentPlatformMap.keySet().isEmpty()) {
                    // 事件没有要通知的上级，那么需要通知所有订阅了的上级
                    Map<ParentPlatform, List<CommonGbChannel>> catalogData = new HashMap<>();
                    for (CommonGbChannel channel : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(channel);
                        if (parentPlatforms != null && parentPlatforms.size() > 0) {
                            for (ParentPlatform platform : parentPlatforms) {
                                if (!catalogData.containsKey(platform)) {
                                    catalogData.put(platform, new ArrayList<>());
                                }
                                catalogData.get(platform).add(channel);
                            }
                        }
                    }
                    for (ParentPlatform platform : catalogData.keySet()) {
                        SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getId());
                        if (subscribeInfo == null) {
                            continue;
                        }
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), platform.getServerGBId(), catalogData.get(platform).size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), platform, catalogData.get(platform), subscribeInfo, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
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
                    if (!event.getChannels().isEmpty()) {
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), event.getPlatformId(), event.getChannels().size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), parentPlatform, event.getChannels(), subscribe, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                        }
                    }
                }else if (!parentPlatformMap.keySet().isEmpty()) {
                    // 事件没有要通知的上级，那么需要通知所有订阅了的上级
                    Map<ParentPlatform, List<CommonGbChannel>> catalogData = new HashMap<>();
                    for (CommonGbChannel channel : parentPlatformMap.keySet()) {
                        List<ParentPlatform> parentPlatforms = parentPlatformMap.get(channel);
                        if (parentPlatforms != null && parentPlatforms.size() > 0) {
                            for (ParentPlatform platform : parentPlatforms) {
                                if (!catalogData.containsKey(platform)) {
                                    catalogData.put(platform, new ArrayList<>());
                                }
                                catalogData.get(platform).add(channel);
                            }
                        }
                    }
                    for (ParentPlatform platform : catalogData.keySet()) {
                        SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getId());
                        if (subscribeInfo == null) {
                            continue;
                        }
                        logger.info("[Catalog事件: {}]平台：{}，影响通道{}个", event.getType(), platform.getServerGBId(), catalogData.get(platform).size());
                        try {
                            sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), platform, catalogData.get(platform), subscribeInfo, null);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            logger.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
}
 