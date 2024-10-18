package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
 * 监听：  SUBSCRIBE VM_MSG_GPS
 * 发布   PUBLISH VM_MSG_GPS '{"messageId":"1727228507555","id":"24212345671381000047","lng":116.30307666666667,"lat":40.03295833333333,"time":"2024-09-25T09:41:47","direction":"56.0","speed":0.0,"altitude":60.0,"unitNo":"100000000","memberNo":"10000047"}'
 */
@Slf4j
@Component
public class RedisGpsMsgListener implements MessageListener {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMobilePositionService mobilePositionService;

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
                        log.info("[REDIS的位置变化通知], {}", JSON.toJSONString(gpsMsgInfo));
                        // 只是放入redis缓存起来
                        redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
                    }catch (Exception e) {
                        log.warn("[REDIS的位置变化通知] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        log.error("[REDIS的位置变化通知] 异常内容： ", e);
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
        List<GPSMsgInfo> gpsMsgInfoList = redisCatchStorage.getAllGpsMsgInfo();
        if (!gpsMsgInfoList.isEmpty()) {
            mobilePositionService.updateStreamGPS(gpsMsgInfoList);
            for (GPSMsgInfo msgInfo : gpsMsgInfoList) {
                msgInfo.setStored(true);
                redisCatchStorage.updateGpsMsgInfo(msgInfo);
            }
        }
    }
}
