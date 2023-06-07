package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.service.IStreamPushService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收来自redis的关闭流更新通知
 * @author lin
 */
@Component
public class RedisCloseStreamMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisCloseStreamMsgListener.class);


    @Autowired
    private IStreamPushService pushService;

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
                        JSONObject jsonObject = JSON.parseObject(msg.getBody());
                        String app = jsonObject.getString("app");
                        String stream = jsonObject.getString("stream");
                        pushService.stop(app, stream);

                    }catch (Exception e) {
                        logger.warn("[REDIS的关闭推流通知] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[REDIS的关闭推流通知] 异常内容： ", e);
                    }
                }
            });
        }
    }
}
