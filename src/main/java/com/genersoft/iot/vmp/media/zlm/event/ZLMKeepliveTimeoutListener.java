package com.genersoft.iot.vmp.media.zlm.event;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.RedisKeyExpirationEventMessageListener;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**    
 * @description:设备心跳超时监听,借助redis过期特性，进行监听，监听到说明设备心跳超时，发送离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:35:46     
 */
@Component
public class ZLMKeepliveTimeoutListener extends RedisKeyExpirationEventMessageListener {

    private Logger logger = LoggerFactory.getLogger(ZLMKeepliveTimeoutListener.class);

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private ZLMRESTfulUtils zlmresTfulUtils;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IMediaServerService mediaServerService;

    public ZLMKeepliveTimeoutListener(RedisMessageListenerContainer listenerContainer, UserSetting userSetting) {
        super(listenerContainer, userSetting);
    }


    /**
     * 监听失效的key，key格式为keeplive_deviceId
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //  获取失效的key
        String expiredKey = message.toString();
        String KEEPLIVEKEY_PREFIX = VideoManagerConstants.MEDIA_SERVER_KEEPALIVE_PREFIX + userSetting.getServerId() + "_";
        if(!expiredKey.startsWith(KEEPLIVEKEY_PREFIX)){
        	return;
        }
        
        String mediaServerId = expiredKey.substring(KEEPLIVEKEY_PREFIX.length(),expiredKey.length());
        logger.info("[zlm心跳到期]：" + mediaServerId);
        // 发起http请求验证zlm是否确实无法连接，如果确实无法连接则发送离线事件，否则不作处理
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        JSONObject mediaServerConfig = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (mediaServerConfig == null) {
            publisher.zlmOfflineEventPublish(mediaServerId);
        }else {
            logger.info("[zlm心跳到期]：{}验证后zlm仍在线，恢复心跳信息", mediaServerId);
            // 添加zlm信息
            mediaServerService.updateMediaServerKeepalive(mediaServerId, mediaServerConfig);
        }

    }
}
