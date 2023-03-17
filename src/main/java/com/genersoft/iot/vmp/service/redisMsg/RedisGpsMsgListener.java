package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
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

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        GPSMsgInfo gpsMsgInfo = JSON.parseObject(msg.getBody(), GPSMsgInfo.class);
                        // 只是放入redis缓存起来
                        redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
                    }catch (Exception e) {
                        logger.warn("[REDIS的ALARM通知] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[REDIS的ALARM通知] 异常内容： ", e);
                    }
                }
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
