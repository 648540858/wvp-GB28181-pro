package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;

import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;


/**
 * 接收其他wvp发送流变化通知
 * @author lin
 */
@Component
public class RedisStreamMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisStreamMsgListener.class);

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IVideoManagerStorage storage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    @Override
    public void onMessage(Message message, byte[] bytes) {

        JSONObject steamMsgJson = JSON.parseObject(message.getBody(), JSONObject.class);
        if (steamMsgJson == null) {
            logger.warn("[收到redis 流变化]消息解析失败");
            return;
        }
        String serverId = steamMsgJson.getString("serverId");

        if (userSetting.getServerId().equals(serverId)) {
            // 自己发送的消息忽略即可
            return;
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
}
