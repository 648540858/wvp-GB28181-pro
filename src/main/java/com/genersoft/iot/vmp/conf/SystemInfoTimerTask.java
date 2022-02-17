package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 获取系统信息写入redis
 */
@Component
public class SystemInfoTimerTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Scheduled(fixedRate = 1000)   //每1秒执行一次
    public void execute(){
        try {
            double cpuInfo = SystemInfoUtils.getCpuInfo();
            redisCatchStorage.addCpuInfo(cpuInfo);
            double memInfo = SystemInfoUtils.getMemInfo();
            redisCatchStorage.addMemInfo(memInfo);
            Map<String, String> networkInterfaces = SystemInfoUtils.getNetworkInterfaces();
            redisCatchStorage.addNetInfo(networkInterfaces);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
