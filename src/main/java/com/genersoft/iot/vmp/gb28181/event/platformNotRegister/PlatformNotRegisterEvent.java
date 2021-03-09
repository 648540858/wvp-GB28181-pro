package com.genersoft.iot.vmp.gb28181.event.platformNotRegister;

import org.springframework.context.ApplicationEvent;

public class PlatformNotRegisterEvent extends ApplicationEvent {

    /**
     * Add default serial version ID
     */
    private static final long serialVersionUID = 1L;
    
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
