package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 获取系统信息写入redis
 */
@Slf4j
@Component
public class SystemInfoTimerTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Scheduled(fixedRate = 2000)   //每1秒执行一次
    public void execute(){
        try {
            double cpuInfo = SystemInfoUtils.getCpuInfo();
            redisCatchStorage.addCpuInfo(cpuInfo);
            double memInfo = SystemInfoUtils.getMemInfo();
            redisCatchStorage.addMemInfo(memInfo);
            Map<String, Double> networkInterfaces = SystemInfoUtils.getNetworkInterfaces();
            redisCatchStorage.addNetInfo(networkInterfaces);
            List<Map<String, Object>> diskInfo =SystemInfoUtils.getDiskInfo();
            redisCatchStorage.addDiskInfo(diskInfo);
        } catch (InterruptedException e) {
            log.error("[获取系统信息失败] {}", e.getMessage());
        }

    }


}
