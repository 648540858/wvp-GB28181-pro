package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamType;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannelResponse;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接收redis发送的结束推流请求
 * @author lin
 */
@Component
public class RedisPushStreamCloseResponseListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisPushStreamCloseResponseListener.class);

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private ISIPCommanderForPlatform commanderFroPlatform;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ZLMServerFactory zlmServerFactory;


    private Map<String, PushStreamResponseEvent> responseEvents = new ConcurrentHashMap<>();

    public interface PushStreamResponseEvent{
        void run(MessageForPushChannelResponse response);
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.info("[REDIS消息-推流结束]： {}", new String(message.getBody()));
        MessageForPushChannel pushChannel = JSON.parseObject(message.getBody(), MessageForPushChannel.class);
        StreamPushItem push = streamPushService.getPush(pushChannel.getApp(), pushChannel.getStream());
        if (push != null) {
            if (redisCatchStorage.isChannelSendingRTP(push.getGbId())) {
                List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByChnnelId(
                        push.getGbId());
                if (sendRtpItems.size() > 0) {
                    for (SendRtpItem sendRtpItem : sendRtpItems) {
                        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                        // 停止向上级推流
                        String streamId = sendRtpItem.getStreamId();
                        Map<String, Object> param = new HashMap<>();
                        param.put("vhost","__defaultVhost__");
                        param.put("app",sendRtpItem.getApp());
                        param.put("stream",streamId);
                        param.put("ssrc",sendRtpItem.getSsrc());
                        logger.info("[REDIS消息-推流结束] 停止向上级推流：{}", streamId);
                        MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                        redisCatchStorage.deleteSendRTPServer(sendRtpItem.getPlatformId(), sendRtpItem.getChannelId(), sendRtpItem.getCallId(), sendRtpItem.getStreamId());
                        zlmServerFactory.stopSendRtpStream(mediaInfo, param);

                        try {
                            commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                        }
                        if (InviteStreamType.PUSH == sendRtpItem.getPlayType()) {
                            MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
                                    sendRtpItem.getApp(), sendRtpItem.getStreamId(), sendRtpItem.getChannelId(),
                                    sendRtpItem.getPlatformId(), parentPlatform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
                            messageForPushChannel.setPlatFormIndex(parentPlatform.getId());
                            redisCatchStorage.sendPlatformStopPlayMsg(messageForPushChannel);
                        }
                    }
                }
            }
        }

    }

    public void addEvent(String app, String stream, PushStreamResponseEvent callback) {
        responseEvents.put(app + stream, callback);
    }

    public void removeEvent(String app, String stream) {
        responseEvents.remove(app + stream);
    }
}
