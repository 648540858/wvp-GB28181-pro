package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 接收来自redis的GPS更新通知
 *
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
    private IStreamPushService streamPushService;

    @Autowired
    private IGbChannelService channelService;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();


    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        log.debug("[REDIS: GPS]： {}", new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 200, timeUnit = TimeUnit.MILLISECONDS)   //每400毫秒执行一次
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<Message> messageDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            Message msg = taskQueue.poll();
            if (msg != null) {
                messageDataList.add(msg);
            }
        }
        if (messageDataList.isEmpty()) {
            return;
        }
        for (Message msg : messageDataList) {
            try {
                GPSMsgInfo gpsMsgInfo = JSON.parseObject(msg.getBody(), GPSMsgInfo.class);
                gpsMsgInfo.setTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(gpsMsgInfo.getTime()));
                log.info("[REDIS的位置变化通知], {}", JSON.toJSONString(gpsMsgInfo));
                // 只是放入redis缓存起来
                redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
            } catch (Exception e) {
                log.warn("[REDIS的位置变化通知] 发现未处理的异常, \r\n{}", JSON.toJSONString(msg));
                log.error("[REDIS的位置变化通知] 异常内容： ", e);
            }
        }
    }

    /**
     * 定时将经纬度更新到数据库
     */
    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)   //每2秒执行一次
    public void execute() {
        // 需要查询到
        List<GPSMsgInfo> gpsMsgInfoList = redisCatchStorage.getAllGpsMsgInfo();
        if (!gpsMsgInfoList.isEmpty()) {
            gpsMsgInfoList = gpsMsgInfoList.stream().filter(gpsMsgInfo -> !gpsMsgInfo.isStored()).collect(Collectors.toList());;
            if (!gpsMsgInfoList.isEmpty()) {
                channelService.updateGPSFromGPSMsgInfo(gpsMsgInfoList);
                for (GPSMsgInfo msgInfo : gpsMsgInfoList) {
                    msgInfo.setStored(true);
                    redisCatchStorage.updateGpsMsgInfo(msgInfo);
                }
            }
        }
    }
}
