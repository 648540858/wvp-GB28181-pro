package com.genersoft.iot.vmp.gb28181.event.alarm;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import org.springframework.context.ApplicationEvent;

/**
 * @description: 报警事件
 * @author: lawrencehj
 * @data: 2021-01-20
 */

public class AlarmEvent extends ApplicationEvent {
    public AlarmEvent(Object source) {
        super(source);
    }

    private DeviceAlarm deviceAlarm;

    public DeviceAlarm getAlarmInfo() {
        return deviceAlarm;
    }
    
    public void setAlarmInfo(DeviceAlarm deviceAlarm) {
        this.deviceAlarm = deviceAlarm;
    }
}
