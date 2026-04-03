package com.genersoft.iot.vmp.gb28181.event.alarm;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.util.List;

/**
 * @description: 报警事件
 * @author: lawrencehj
 * @data: 2021-01-20
 */
@Getter
@Setter
public class DeviceAlarmEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceAlarmEvent(Object source) {
        super(source);
    }

    private List<DeviceAlarmNotify> deviceAlarmList;

}
