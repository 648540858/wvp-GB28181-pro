package com.genersoft.iot.vmp.jt1078.event.eventListener;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.event.ConnectChangeEvent;
import com.genersoft.iot.vmp.jt1078.event.RegisterEvent;
import com.genersoft.iot.vmp.jt1078.proc.request.J0003;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ConnectChangeEventListener implements ApplicationListener<ConnectChangeEvent>  {

    private final static Logger log = LoggerFactory.getLogger(ConnectChangeEventListener.class);

    @Autowired
    private Ijt1078Service service;

    @Override
    public void onApplicationEvent(ConnectChangeEvent event) {
        if (event.isConnected()) {
            log.info("[JT-设备已连接] 终端ID： {}", event.getPhoneNumber());
        }else{
            log.info("[JT-设备连接已断开] 终端ID： {}", event.getPhoneNumber());
            if(SessionManager.INSTANCE.get(event.getPhoneNumber()) != null) {
                SessionManager.INSTANCE.get(event.getPhoneNumber()).unregister();
            }
        }
        JTDevice device = service.getDevice(event.getPhoneNumber());
        if (device != null) {
            service.updateDeviceStatus(event.isConnected(), event.getPhoneNumber());
        }
    }
}
