package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogSubscribePutEventLister implements ApplicationListener<CatalogSubscribePutEvent> {

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private EventPublisher eventPublisher;


    @Override
    public void onApplicationEvent(CatalogSubscribePutEvent event) {

        Platform platform = platformService.queryPlatformByServerGBId(event.getPlatformId());
        if (platform == null){
            return;
        }

        CommonGBChannel channel = CommonGBChannel.build(platform);

        // 发送消息
        try {
            // 发送catalog
            eventPublisher.catalogEventPublish(platform.getId(), channel, CatalogEvent.ADD);
        } catch (Exception e) {
            log.warn("[推送平台信息] 发送失败，平台{}（{}）", platform.getName(), platform.getServerGBId(), e);
        }
    }
}
