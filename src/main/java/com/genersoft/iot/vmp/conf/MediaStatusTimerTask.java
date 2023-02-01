package com.genersoft.iot.vmp.conf;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时向zlm同步媒体流状态
 */
public class MediaStatusTimerTask {


    @Scheduled(fixedRate = 2 * 1000)   //每3秒执行一次
    public void execute(){

    }
}
