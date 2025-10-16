package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.bean.JTPositionBaseInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 设备更新事件
 */
public class JTPositionEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JTPositionEvent(Object source) {
        super(source);
    }

    @Getter
    @Setter
    private String phoneNumber;

    @Getter
    @Setter
    private JTPositionBaseInfo positionInfo;
}
