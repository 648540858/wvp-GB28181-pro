package com.genersoft.iot.vmp.conf.runner;

import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;


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
        // 读取redis没有心跳信息的则设置为离线，等收到下次心跳设置为在线
        // 设置所有设备离线
        storager.outlineForAll();
        List<String> onlineForAll = redisCatchStorage.getOnlineForAll();
        for (String deviceId : onlineForAll) {
            storager.online(deviceId);
        }

        // TODO 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
    }
}
