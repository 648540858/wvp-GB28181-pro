package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;

import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
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
 * 接收其他wvp发送流变化通知
 * @author lin
 */
@Component
public class RedisStreamMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisStreamMsgListener.class);

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    private boolean taskQueueHandlerRun = false;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(Message message, byte[] bytes) {

        taskQueue.offer(message);
        if (!taskQueueHandlerRun) {
            taskQueueHandlerRun = true;
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    JSONObject steamMsgJson = JSON.parseObject(msg.getBody(), JSONObject.class);
                    if (steamMsgJson == null) {
                        logger.warn("[收到redis 流变化]消息解析失败");
                        continue;
                    }
                    String serverId = steamMsgJson.getString("serverId");

                    if (userSetting.getServerId().equals(serverId)) {
                        // 自己发送的消息忽略即可
                        continue;
                    }
                    logger.info("[收到redis 流变化]： {}", new String(message.getBody()));
                    String app = steamMsgJson.getString("app");
                    String stream = steamMsgJson.getString("stream");
                    boolean register = steamMsgJson.getBoolean("register");
                    String mediaServerId = steamMsgJson.getString("mediaServerId");
                    MediaItem mediaItem = new MediaItem();
                    mediaItem.setSeverId(serverId);
                    mediaItem.setApp(app);
                    mediaItem.setStream(stream);
                    mediaItem.setRegist(register);
                    mediaItem.setMediaServerId(mediaServerId);
                    mediaItem.setCreateStamp(System.currentTimeMillis()/1000);
                    mediaItem.setAliveSecond(0L);
                    mediaItem.setTotalReaderCount("0");
                    mediaItem.setOriginType(0);
                    mediaItem.setOriginTypeStr("0");
                    mediaItem.setOriginTypeStr("unknown");
                    if (register) {
                        zlmMediaListManager.addPush(mediaItem);
                    }else {
                        zlmMediaListManager.removeMedia(app, stream);
                    }
                }
                taskQueueHandlerRun = false;
            });
        }
    }
}
