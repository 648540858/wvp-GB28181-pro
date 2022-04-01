package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 定时查找redis中的GPS推送消息，并保存到对应的流中
 */
@Component
public class StreamGPSSubscribeTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;



    @Scheduled(fixedRate = 30 * 1000)   //每30秒执行一次
    public void execute(){
        List<GPSMsgInfo> gpsMsgInfo = redisCatchStorage.getAllGpsMsgInfo();
        if (gpsMsgInfo.size() > 0) {
            storager.updateStreamGPS(gpsMsgInfo);
            for (GPSMsgInfo msgInfo : gpsMsgInfo) {
                msgInfo.setStored(true);
                redisCatchStorage.updateGpsMsgInfo(msgInfo);
            }
        }

    }
}
