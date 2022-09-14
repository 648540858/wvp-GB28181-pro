package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.ChannelOnlineEvent;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannelResponse;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接收redis返回的推流结果
 * @author lin
 */
@Component
public class RedisPushStreamResponseListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisPushStreamResponseListener.class);

    private Map<String, PushStreamResponseEvent> responseEvents = new ConcurrentHashMap<>();

    public interface PushStreamResponseEvent{
        void run(MessageForPushChannelResponse response);
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        //
        logger.warn("[REDIS消息-请求推流结果]： {}", new String(message.getBody()));
        MessageForPushChannelResponse response = JSON.parseObject(new String(message.getBody()), MessageForPushChannelResponse.class);
        if (response == null || ObjectUtils.isEmpty(response.getApp()) || ObjectUtils.isEmpty(response.getStream())){
            logger.info("[REDIS消息-请求推流结果]：参数不全");
            return;
        }
        // 查看正在等待的invite消息
        if (responseEvents.get(response.getApp() + response.getStream()) != null) {
            responseEvents.get(response.getApp() + response.getStream()).run(response);
        }
    }

    public void addEvent(String app, String stream, PushStreamResponseEvent callback) {
        responseEvents.put(app + stream, callback);
    }

    public void removeEvent(String app, String stream) {
        responseEvents.remove(app + stream);
    }
}
