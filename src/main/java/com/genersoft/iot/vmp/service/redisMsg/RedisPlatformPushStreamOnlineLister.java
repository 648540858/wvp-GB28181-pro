package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannelResponse;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamPushMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author lin
 */
@Component
public class RedisPlatformPushStreamOnlineLister implements MessageListener {

    private final Logger logger = LoggerFactory.getLogger("RedisPlatformPushStreamOnlineLister");

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 通过redis消息接收流上线的通知，如果本机由对这个流的监听，则回调
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    SendRtpItem sendRtpItem = JSON.parseObject(new String(msg.getBody()), SendRtpItem.class);
                    sendStreamEvent(sendRtpItem);
                }
            });
        }
    }

    private final Map<String, ChannelOnlineEvent> channelOnPublishEvents = new ConcurrentHashMap<>();

    public void sendStreamEvent(SendRtpItem sendRtpItem) {
        // 查看推流状态
        ChannelOnlineEvent channelOnlineEventLister = getChannelOnlineEventLister(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (channelOnlineEventLister != null)  {
            try {
                channelOnlineEventLister.run(sendRtpItem);
            } catch (ParseException e) {
                logger.error("sendStreamEvent: ", e);
            }
            removedChannelOnlineEventLister(sendRtpItem.getApp(), sendRtpItem.getStream());
        }
    }

    public void addChannelOnlineEventLister(String app, String stream, ChannelOnlineEvent callback) {
        this.channelOnPublishEvents.put(app + "_" + stream, callback);
    }

    public void removedChannelOnlineEventLister(String app, String stream) {
        this.channelOnPublishEvents.remove(app + "_" + stream);
    }

    public ChannelOnlineEvent getChannelOnlineEventLister(String app, String stream) {
        return this.channelOnPublishEvents.get(app + "_" + stream);
    }


}
