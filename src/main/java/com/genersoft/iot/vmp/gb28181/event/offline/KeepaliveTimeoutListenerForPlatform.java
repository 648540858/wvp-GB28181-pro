package com.genersoft.iot.vmp.gb28181.event.offline;

import com.genersoft.iot.vmp.conf.RedisKeyExpirationEventMessageListener;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;

/**    
 * @description:设备心跳超时监听,借助redis过期特性，进行监听，监听到说明设备心跳超时，发送离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:35:46     
 */
@Component
public class KeepaliveTimeoutListenerForPlatform extends RedisKeyExpirationEventMessageListener {

    private Logger logger = LoggerFactory.getLogger(KeepaliveTimeoutListenerForPlatform.class);

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private SipSubscribe sipSubscribe;

	@Autowired
	private IVideoManagerStorage storager;

    public KeepaliveTimeoutListenerForPlatform(RedisMessageListenerContainer listenerContainer, UserSetting userSetting) {
        super(listenerContainer, userSetting);
    }


    /**
     * 监听失效的key
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //  获取失效的key
        String expiredKey = message.toString();
        // 平台心跳到期,需要重发, 判断是否已经多次未收到心跳回复, 多次未收到,则重新发起注册, 注册尝试多次未得到回复,则认为平台离线
        String PLATFORM_KEEPLIVEKEY_PREFIX = VideoManagerConstants.PLATFORM_KEEPALIVE_PREFIX + userSetting.getServerId() + "_";
        String PLATFORM_REGISTER_PREFIX = VideoManagerConstants.PLATFORM_REGISTER_PREFIX + userSetting.getServerId() + "_";
        String KEEPLIVEKEY_PREFIX = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetting.getServerId() + "_";
        String REGISTER_INFO_PREFIX = VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_";
        if (expiredKey.startsWith(PLATFORM_KEEPLIVEKEY_PREFIX)) {
            String platformGBId = expiredKey.substring(PLATFORM_KEEPLIVEKEY_PREFIX.length(),expiredKey.length());
            ParentPlatform platform = storager.queryParentPlatByServerGBId(platformGBId);
            if (platform != null) {
                publisher.platformKeepaliveExpireEventPublish(platformGBId);
            }
        }else if (expiredKey.startsWith(PLATFORM_REGISTER_PREFIX)) {
            String platformGBId = expiredKey.substring(PLATFORM_REGISTER_PREFIX.length(),expiredKey.length());
            ParentPlatform platform = storager.queryParentPlatByServerGBId(platformGBId);
            if (platform != null) {
                publisher.platformRegisterCycleEventPublish(platformGBId);
            }
        }else if (expiredKey.startsWith(KEEPLIVEKEY_PREFIX)){
            String deviceId = expiredKey.substring(KEEPLIVEKEY_PREFIX.length(),expiredKey.length());
            Device device = storager.queryVideoDevice(deviceId);
            if (device != null) {
                publisher.outlineEventPublish(deviceId, KEEPLIVEKEY_PREFIX);
            }
        }else if (expiredKey.startsWith(REGISTER_INFO_PREFIX)) {
            String callId = expiredKey.substring(REGISTER_INFO_PREFIX.length());
            if (sipSubscribe.getErrorSubscribe(callId) != null) {
                SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
                eventResult.callId = callId;
                eventResult.msg = "注册超时";
                eventResult.type = "register timeout";
                sipSubscribe.getErrorSubscribe(callId).response(eventResult);
            }
        }

    }
}
