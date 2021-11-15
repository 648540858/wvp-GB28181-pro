package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WVPTimerTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private MediaConfig mediaConfig;

//    @Scheduled(cron="0/2 * *  * * ? ")   //每3秒执行一次
//    public void execute(){
////        redisCatchStorage.updateWVPInfo();
//    }
}
