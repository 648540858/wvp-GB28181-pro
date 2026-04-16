package com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;


public class MobilePositionEvent extends ApplicationEvent {
    public MobilePositionEvent(Object source) {
        super(source);
    }

    @Getter
    @Setter
    private MobilePosition mobilePosition;

    @Getter
    @Setter
    private List<MobilePosition> mobilePositionList;
}
