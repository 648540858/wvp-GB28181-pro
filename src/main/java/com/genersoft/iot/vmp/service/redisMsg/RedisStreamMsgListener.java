package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;

import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
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

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
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
                        OnStreamChangedHookParam onStreamChangedHookParam = new OnStreamChangedHookParam();
                        onStreamChangedHookParam.setSeverId(serverId);
                        onStreamChangedHookParam.setApp(app);
                        onStreamChangedHookParam.setStream(stream);
                        onStreamChangedHookParam.setRegist(register);
                        onStreamChangedHookParam.setMediaServerId(mediaServerId);
                        onStreamChangedHookParam.setCreateStamp(System.currentTimeMillis()/1000);
                        onStreamChangedHookParam.setAliveSecond(0L);
                        onStreamChangedHookParam.setTotalReaderCount("0");
                        onStreamChangedHookParam.setOriginType(0);
                        onStreamChangedHookParam.setOriginTypeStr("0");
                        onStreamChangedHookParam.setOriginTypeStr("unknown");
                        if (register) {
                            zlmMediaListManager.addPush(onStreamChangedHookParam);
                        }else {
                            zlmMediaListManager.removeMedia(app, stream);
                        }
                    }catch (Exception e) {
                        logger.warn("[REDIS消息-流变化] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[REDIS消息-流变化] 异常内容： ", e);
                    }
                }
            });
        }
    }
}
