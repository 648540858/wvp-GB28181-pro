package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * 系统启动时控制设备离线
 */
@Component
@Order(value=4)
public class SipDeviceRunner implements CommandLineRunner {

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void run(String... args) throws Exception {
        // 设置所有设备离线
        storager.outlineForAll();
        // 设置所有设备离线
        redisCatchStorage.outlineForAll();
    }
}
