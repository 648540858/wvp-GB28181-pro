package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisGPSMsgListener implements MessageListener {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        GPSMsgInfo gpsMsgInfo = JSON.parseObject(message.getBody(), GPSMsgInfo.class);
        System.out.println(JSON.toJSON(gpsMsgInfo));
        redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
    }
}
