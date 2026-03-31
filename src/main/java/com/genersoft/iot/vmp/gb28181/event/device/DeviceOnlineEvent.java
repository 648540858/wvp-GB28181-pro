package com.genersoft.iot.vmp.gb28181.event.device;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

@Getter
@Setter
public class DeviceOnlineEvent extends ApplicationEvent {

    private Device device;

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceOnlineEvent(Object source) {
        super(source);
    }
}
