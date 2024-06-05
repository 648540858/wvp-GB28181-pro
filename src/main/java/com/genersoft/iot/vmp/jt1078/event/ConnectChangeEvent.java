package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

/**
 * 链接断或者连接的事件
 */

public class ConnectChangeEvent extends ApplicationEvent {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConnectChangeEvent(Object source) {
        super(source);
    }


    private boolean connected;

    private String phoneNumber;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
