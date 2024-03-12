package com.genersoft.iot.vmp.jt1078.event.eventListener;

import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEvent;
import com.genersoft.iot.vmp.jt1078.event.RegisterEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class RegisterEventListener implements ApplicationListener<RegisterEvent>  {

    @Override
    public void onApplicationEvent(RegisterEvent event) {
        System.out.println("收到设备注册： "+ event.getDeviceId());
    }
}
