package com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import org.springframework.context.ApplicationEvent;

public class MobilePositionEvent extends ApplicationEvent {
    public MobilePositionEvent(Object source) {
        super(source);
    }

    private MobilePosition mobilePosition;

    public MobilePosition getMobilePosition() {
        return mobilePosition;
    }

    public void setMobilePosition(MobilePosition mobilePosition) {
        this.mobilePosition = mobilePosition;
    }
}
