package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

public class CatalogSubscribePutEvent extends ApplicationEvent {

    public CatalogSubscribePutEvent(Object source) {
        super(source);
    }

    @Getter
    @Setter
    private SubscribeInfo subscribeInfo;

    @Getter
    @Setter
    private String platformId;
}
