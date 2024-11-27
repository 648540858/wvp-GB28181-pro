package com.genersoft.iot.vmp.conf;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WVPTimerTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private SipConfig sipConfig;

    @Scheduled(fixedDelay = 2 * 1000)   //每3秒执行一次
    public void execute(){
        log.info("[更新服务信息]");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ip", sipConfig.getShowIp());
        jsonObject.put("port", serverPort);
        redisCatchStorage.updateWVPInfo(jsonObject, 3);
    }
}
