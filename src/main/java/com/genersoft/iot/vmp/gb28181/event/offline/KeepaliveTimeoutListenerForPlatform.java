package com.genersoft.iot.vmp.gb28181.event.offline;

import com.genersoft.iot.vmp.conf.RedisKeyExpirationEventMessageListener;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import org.springframework.util.StringUtils;

import java.util.Properties;

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
	private UserSetup userSetup;

	@Autowired
	private SipSubscribe sipSubscribe;

	@Autowired
	private IVideoManagerStorager storager;

    public KeepaliveTimeoutListenerForPlatform(RedisMessageListenerContainer listenerContainer, UserSetup userSetup) {
        super(listenerContainer, userSetup);
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
        logger.debug(expiredKey);
        // 平台心跳到期,需要重发, 判断是否已经多次未收到心跳回复, 多次未收到,则重新发起注册, 注册尝试多次未得到回复,则认为平台离线
        String PLATFORM_KEEPLIVEKEY_PREFIX = VideoManagerConstants.PLATFORM_KEEPALIVE_PREFIX + userSetup.getServerId() + "_";
        String PLATFORM_REGISTER_PREFIX = VideoManagerConstants.PLATFORM_REGISTER_PREFIX + userSetup.getServerId() + "_";
        String KEEPLIVEKEY_PREFIX = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetup.getServerId() + "_";
        String REGISTER_INFO_PREFIX = VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetup.getServerId() + "_";
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
            String callid = expiredKey.substring(REGISTER_INFO_PREFIX.length());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.callId = callid;
            eventResult.msg = "注册超时";
            eventResult.type = "register timeout";
            sipSubscribe.getErrorSubscribe(callid).response(eventResult);
        }

    }
}
