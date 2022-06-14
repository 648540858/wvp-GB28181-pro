package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 接收来自redis的GPS更新通知
 * @author lin
 */
@Component
public class RedisGpsMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisGpsMsgListener.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        if (logger.isDebugEnabled()) {
            logger.debug("收到来自REDIS的GPS通知： {}", new String(message.getBody()));
        }
        GPSMsgInfo gpsMsgInfo = JSON.parseObject(message.getBody(), GPSMsgInfo.class);
        redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
    }
}
