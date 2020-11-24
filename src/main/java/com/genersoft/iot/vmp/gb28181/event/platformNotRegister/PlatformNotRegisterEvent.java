package com.genersoft.iot.vmp.gb28181.event.platformNotRegister;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import org.springframework.context.ApplicationEvent;

public class PlatformNotRegisterEvent extends ApplicationEvent {

    private String platformGbID;

    public PlatformNotRegisterEvent(Object source) {
        super(source);
    }

    public String getPlatformGbID() {
        return platformGbID;
    }

    public void setPlatformGbID(String platformGbID) {
        this.platformGbID = platformGbID;
    }
}
