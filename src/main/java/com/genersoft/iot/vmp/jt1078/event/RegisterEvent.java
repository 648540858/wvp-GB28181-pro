package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 注册事件
 */
public class RegisterEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public RegisterEvent(Object source) {
        super(source);
    }

    @Getter
    @Setter
    private JTDevice device;
}
