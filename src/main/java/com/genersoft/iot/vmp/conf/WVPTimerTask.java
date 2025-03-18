package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class WVPTimerTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private SipConfig sipConfig;

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)   //每3秒执行一次
    public void execute(){
        redisCatchStorage.updateWVPInfo(ServerInfo.create(sipConfig.getShowIp(), serverPort), 3);
    }
}
