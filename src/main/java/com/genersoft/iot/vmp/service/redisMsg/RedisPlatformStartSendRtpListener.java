package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 收到消息后开始给上级发流
 * @author lin
 */
@Component
public class RedisPlatformStartSendRtpListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisPlatformStartSendRtpListener.class);

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.info("[REDIS消息-收到上级等到设备推流的redis消息]： {}", new String(message.getBody()));
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        MessageForPushChannel messageForPushChannel = JSON.parseObject(new String(msg.getBody()), MessageForPushChannel.class);
                        if (messageForPushChannel == null
                                || ObjectUtils.isEmpty(messageForPushChannel.getApp())
                                || ObjectUtils.isEmpty(messageForPushChannel.getStream())
                        || userSetting.getServerId().equals(messageForPushChannel.getServerId())){
                            continue;
                        }

                        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
                        HookSubscribeForStreamChange hook = HookSubscribeFactory.on_stream_changed(
                                messageForPushChannel.getApp(), messageForPushChannel.getStream(), true, "rtsp",
                                null);
                        hookSubscribe.addSubscribe(hook, (MediaServerItem mediaServerItemInUse, HookParam hookParam) -> {
                            // 读取redis中的上级点播信息，生成sendRtpItm发送出去

                        });


                    }catch (Exception e) {
                        logger.warn("[REDIS消息-请求推流结果] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[REDIS消息-请求推流结果] 异常内容： ", e);
                    }
                }
            });
        }
    }
}
