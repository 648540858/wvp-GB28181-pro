package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

/**
 * 链接断或者连接的事件
 */

@Setter
@Getter
public class ConnectChangeEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public ConnectChangeEvent(Object source) {
        super(source);
    }


    private boolean connected;

    private String phoneNumber;

}
