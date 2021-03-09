package com.genersoft.iot.vmp.gb28181.event.platformKeepaliveExpire;

import org.springframework.context.ApplicationEvent;

/**
 *  平台心跳超时事件
 */
public class PlatformKeepaliveExpireEvent extends ApplicationEvent {

    /**
     * Add default serial version ID
     */
    private static final long serialVersionUID = 1L;
    
    private String platformGbID;

    public PlatformKeepaliveExpireEvent(Object source) {
        super(source);
    }

    public String getPlatformGbID() {
        return platformGbID;
    }

    public void setPlatformGbID(String platformGbID) {
        this.platformGbID = platformGbID;
    }
}
