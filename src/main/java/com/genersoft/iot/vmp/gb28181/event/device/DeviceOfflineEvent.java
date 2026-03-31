package com.genersoft.iot.vmp.gb28181.event.device;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.util.Set;

@Getter
@Setter
public class DeviceOfflineEvent extends ApplicationEvent {

    private Set<String> deviceIds;

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceOfflineEvent(Object source) {
        super(source);
    }
}
