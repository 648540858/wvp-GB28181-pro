package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收来自redis的GPS更新通知
 * @author lin
 */
@Component
public class RedisGpsMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisGpsMsgListener.class);

    private boolean taskQueueHandlerRun = false;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        taskQueue.offer(message);
        if (!taskQueueHandlerRun) {
            taskQueueHandlerRun = true;
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    GPSMsgInfo gpsMsgInfo = JSON.parseObject(msg.getBody(), GPSMsgInfo.class);
                    // 只是放入redis缓存起来
                    redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
                }
                taskQueueHandlerRun = false;
            });
        }
    }

    /**
     * 定时将经纬度更新到数据库
     */
    @Scheduled(fixedRate = 2 * 1000)   //每2秒执行一次
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
