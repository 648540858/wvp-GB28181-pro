package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.platformNotRegister.PlatformNotRegisterEvent;
import com.genersoft.iot.vmp.vmanager.platform.PlatformController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.event.offline.OfflineEvent;
import com.genersoft.iot.vmp.gb28181.event.online.OnlineEvent;

/**    
 * @Description:Event事件通知推送器，支持推送在线事件、离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:30:50     
 */
@Component
public class EventPublisher {

	@Autowired
    private ApplicationEventPublisher applicationEventPublisher;
	
	public void onlineEventPublish(String deviceId, String from) {
		OnlineEvent onEvent = new OnlineEvent(this);
		onEvent.setDeviceId(deviceId);
		onEvent.setFrom(from);
        applicationEventPublisher.publishEvent(onEvent);
    }
	
	public void outlineEventPublish(String deviceId, String from){
		OfflineEvent outEvent = new OfflineEvent(this);
		outEvent.setDeviceId(deviceId);
		outEvent.setFrom(from);
        applicationEventPublisher.publishEvent(outEvent);
    }

	/**
	 * 平台未注册事件
	 * @param platformGbId
	 */
	public void platformNotRegisterEventPublish(String platformGbId){
		PlatformNotRegisterEvent platformNotRegisterEvent = new PlatformNotRegisterEvent(this);
		platformNotRegisterEvent.setPlatformGbID(platformGbId);
        applicationEventPublisher.publishEvent(platformNotRegisterEvent);
    }
}
