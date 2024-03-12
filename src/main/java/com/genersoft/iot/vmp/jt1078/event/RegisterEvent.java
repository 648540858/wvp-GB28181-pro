package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.springframework.context.ApplicationEvent;

/**
 * 注册事件
 */

public class RegisterEvent extends ApplicationEvent {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RegisterEvent(Object source) {
        super(source);
    }


    private JTDevice device;

    public JTDevice getDevice() {
        return device;
    }

    public void setDevice(JTDevice device) {
        this.device = device;
    }
}
