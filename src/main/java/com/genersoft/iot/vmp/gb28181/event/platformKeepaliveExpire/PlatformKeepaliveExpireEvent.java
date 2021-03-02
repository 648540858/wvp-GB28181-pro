package com.genersoft.iot.vmp.gb28181.event.platformKeepaliveExpire;

import org.springframework.context.ApplicationEvent;

/**
 *  平台心跳超时事件
 */
public class PlatformKeepaliveExpireEvent extends ApplicationEvent {

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
