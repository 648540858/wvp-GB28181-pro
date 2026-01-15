package com.genersoft.iot.vmp.gb28181.event.device;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

@Getter
@Setter
public class DeviceOfflineEvent extends ApplicationEvent {

    private String deviceId;

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceOfflineEvent(Object source) {
        super(source);
    }
}
